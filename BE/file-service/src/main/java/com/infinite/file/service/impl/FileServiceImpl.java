package com.infinite.file.service.impl;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.exception.AppException;
import com.infinite.common.util.FileUrlBuilder;
import com.infinite.file.config.FileConfig;
import com.infinite.common.dto.response.FileUploadResponse;
import com.infinite.file.entity.FileResource;
import com.infinite.file.service.FileService;
import com.infinite.file.service.FileResourceService;
import com.infinite.grpc.service.file.FileServiceGrpc;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

import static com.infinite.common.constant.StatusCode.SUCCESS;
import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Service("fileServiceGrpc")
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService, FileServiceGrpc {
    
    private final FileConfig fileConfig;
    private final MinioClient minioClient;
    private final FileResourceService fileResourceService;
    
    @Override
    @Transactional
    public ApiResponse<FileUploadResponse> uploadFile(MultipartFile file, String category, String userId) {
        String objectKey = null;
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new AppException(StatusCode.BAD_REQUEST, message("file.empty"));
            }
            
            if (!isValidFile(file)) {
                String fileExtension = getFileExtension(file.getOriginalFilename());
                String allowedTypesStr = String.join(", ", fileConfig.getAllowedTypes());
                throw new AppException(StatusCode.BAD_REQUEST, 
                    String.format(message("file.invalid.type"), fileExtension, allowedTypesStr));
            }
            
            // Validate file type and size based on category
            validateFileTypeByCategory(file, category);
            
            // Ensure bucket exists
            ensureBucketExists();
            
            // Resolve unique filename BEFORE uploading to MinIO
            String originalFileName = file.getOriginalFilename();
            String parentPath = fileConfig.getBucketName() + "/" + category;
            String uniqueFileName = fileResourceService.resolveUniqueFileName(originalFileName, parentPath);
            
            // Build object key and path with unique filename
            objectKey = category + "/" + uniqueFileName;
            String objectPath = fileConfig.getBucketName() + "/" + objectKey;
            
            // Upload to MinIO with unique filename
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(fileConfig.getBucketName())
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            // Save metadata to database (name already resolved, no duplicate handling needed)
            try {
                fileResourceService.saveFileMetadata(objectPath, file.getContentType(), file.getSize(), userId);
            } catch (Exception e) {
                // Compensation: Delete from MinIO if DB save fails
                log.error("Failed to save metadata to DB, rolling back MinIO upload: ", e);
                try {
                    minioClient.removeObject(
                        RemoveObjectArgs.builder()
                            .bucket(fileConfig.getBucketName())
                            .object(objectKey)
                            .build()
                    );
                } catch (Exception rollbackEx) {
                    log.error("Failed to rollback MinIO upload: ", rollbackEx);
                }
                throw new AppException(StatusCode.INTERNAL_ERROR, "Failed to save file metadata: " + e.getMessage());
            }
            
            // Build file URL lúc response - encode path trước
            String encodedRelativePath = FileUrlBuilder.encodeAndNormalizeForStorage("/" + fileConfig.getBucketName() + "/" + category + "/" + uniqueFileName);
            String fileUrl = buildFullUrl(encodedRelativePath);
            
            FileUploadResponse response = FileUploadResponse.builder()
                    .fileName(uniqueFileName)
                    .originalFileName(file.getOriginalFilename())
                    .fileUrl(fileUrl)  // Full URL for client
                    .relativeUrl(encodedRelativePath)  // Encoded relative URL for service storage
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .category(category)
                    .build();
            
            return ApiResponse.<FileUploadResponse>builder()
                    .code(code(SUCCESS))
                    .message(message("file.upload.success"))
                    .result(response)
                    .build();
                    
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error uploading file to MinIO: ", e);
            // Cleanup MinIO if upload succeeded but other error occurred
            if (objectKey != null) {
                try {
                    minioClient.removeObject(
                        RemoveObjectArgs.builder()
                            .bucket(fileConfig.getBucketName())
                            .object(objectKey)
                            .build()
                    );
                } catch (Exception cleanupEx) {
                    log.error("Failed to cleanup MinIO upload: ", cleanupEx);
                }
            }
            throw new AppException(StatusCode.INTERNAL_ERROR, "Failed to upload file: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public ApiResponse<Object> deleteFile(String fileName, String category) {
        try {
            // Build object path to find metadata
            String objectPath = fileConfig.getBucketName() + "/" + category + "/" + fileName;
            
            // Find metadata in DB first
            FileResource resource = fileResourceService.findByObjectPath(objectPath);
            
            // Extract object key from object path (remove bucket prefix)
            String objectKey = resource.getObjectPath().substring(fileConfig.getBucketName().length() + 1);
            
            // Delete from MinIO
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(fileConfig.getBucketName())
                    .object(objectKey)
                    .build()
            );
            
            // Delete metadata from database
            fileResourceService.deleteFileMetadata(objectPath);
            
            return ApiResponse.builder()
                    .code(code(SUCCESS))
                    .message(message("file.delete.success"))
                    .build();
                    
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting file: ", e);
            throw new AppException(StatusCode.INTERNAL_ERROR, "Failed to delete file: " + e.getMessage());
        }
    }
    
    @Override
    public String getFileUrl(String fileName, String category) {
        try {
            // Build raw object path
            String objectPath = fileConfig.getBucketName() + "/" + category + "/" + fileName;
            
            // Encode path for URL
            String encodedRelativePath = FileUrlBuilder.encodeAndNormalizeForStorage(objectPath);
            
            // Build full URL from encoded relative path
            return buildFullUrl(encodedRelativePath);
        } catch (Exception e) {
            log.error("Error building file URL: ", e);
            return null;
        }
    }
    
    /**
     * Build relative URL (without host/IP, with leading slash) for storage
     * @param relativeUrl Relative path (category/filename)
     * @return Relative URL with bucket prefix and leading slash
     */
    private String buildRelativeUrl(String relativeUrl) {
        return "/" + fileConfig.getBucketName() + "/" + relativeUrl;
    }
    
    /**
     * Build full URL by adding host/IP to relative URL
     * Encodes path segments to handle special characters in file names
     * @param relativeUrl Relative URL stored in DB (/bucket/category/filename)
     * @return Full URL with host/IP and encoded path
     */
    private String buildFullUrl(String encodedRelativePath) {
        try {
            if (encodedRelativePath == null || encodedRelativePath.isEmpty()) {
                return null;
            }
            // Use common utility to ghép host vào encoded relative path
            return FileUrlBuilder.buildFullUrl(fileConfig.getPublicBaseUrl(), encodedRelativePath);
        } catch (Exception e) {
            log.error("Error building full URL: ", e);
            return null;
        }
    }
    
    @Override
    public boolean isValidFile(MultipartFile file) {
        if (file.getContentType() == null) {
            return false;
        }
        
        String[] allowedTypes = fileConfig.getAllowedTypes();
        String fileExtension = getFileExtension(file.getOriginalFilename());
        
        return Arrays.stream(allowedTypes)
                .anyMatch(type -> type.equalsIgnoreCase(fileExtension));
    }
    
    @Override
    public boolean isImageFile(MultipartFile file) {
        String[] imageTypes = {"jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico"};
        String fileExtension = getFileExtension(file.getOriginalFilename());
        
        return Arrays.stream(imageTypes)
                .anyMatch(type -> type.equalsIgnoreCase(fileExtension));
    }
    
    @Override
    public boolean isVideoFile(MultipartFile file) {
        String[] videoTypes = {"mp4", "avi", "mov", "wmv", "flv", "webm", "mkv"};
        String fileExtension = getFileExtension(file.getOriginalFilename());
        
        return Arrays.stream(videoTypes)
                .anyMatch(type -> type.equalsIgnoreCase(fileExtension));
    }
    
    @Override
    public boolean isDocumentFile(MultipartFile file) {
        String[] documentTypes = {"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv"};
        String fileExtension = getFileExtension(file.getOriginalFilename());
        
        return Arrays.stream(documentTypes)
                .anyMatch(type -> type.equalsIgnoreCase(fileExtension));
    }
    
    @Override
    public boolean isArchiveFile(MultipartFile file) {
        String[] archiveTypes = {"zip", "rar", "7z"};
        String fileExtension = getFileExtension(file.getOriginalFilename());
        
        return Arrays.stream(archiveTypes)
                .anyMatch(type -> type.equalsIgnoreCase(fileExtension));
    }
    
    private void ensureBucketExists() throws Exception {
        boolean bucketExists = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(fileConfig.getBucketName())
                .build()
        );
        
        if (!bucketExists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(fileConfig.getBucketName())
                    .build()
            );
            
        }
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Validate file type and size based on category
     * - images/avatar: only image files
     * - videos: only video files
     * - documents: only document files
     * - archives: only archive files
     */
    private void validateFileTypeByCategory(MultipartFile file, String category) {
        if (category == null) {
            // Default validation for null category
            if (file.getSize() > fileConfig.getMaxDefaultSize()) {
                throw new AppException(StatusCode.BAD_REQUEST, 
                    message("file.size.exceeded"));
            }
            return;
        }
        
        String categoryLower = category.toLowerCase();
        long maxSize;
        
        switch (categoryLower) {
            case "images":
                if (!isImageFile(file)) {
                    throw new AppException(StatusCode.BAD_REQUEST, 
                        message("file.category.images.only"));
                }
                maxSize = fileConfig.getMaxImageSize();
                if (file.getSize() > maxSize) {
                    throw new AppException(StatusCode.BAD_REQUEST, 
                        String.format(message("file.size.exceeded.category"), formatFileSize(maxSize)));
                }
                break;
                
            case "avatar":
                if (!isImageFile(file)) {
                    throw new AppException(StatusCode.BAD_REQUEST, 
                        message("file.category.images.only"));
                }
                maxSize = fileConfig.getMaxAvatarSize();
                if (file.getSize() > maxSize) {
                    throw new AppException(StatusCode.BAD_REQUEST, 
                        String.format(message("file.size.exceeded.category"), formatFileSize(maxSize)));
                }
                break;
                
            case "videos":
                if (!isVideoFile(file)) {
                    throw new AppException(StatusCode.BAD_REQUEST, 
                        message("file.category.videos.only"));
                }
                maxSize = fileConfig.getMaxVideoSize();
                if (file.getSize() > maxSize) {
                    throw new AppException(StatusCode.BAD_REQUEST, 
                        String.format(message("file.size.exceeded.category"), formatFileSize(maxSize)));
                }
                break;
                
            case "documents":
                if (!isDocumentFile(file)) {
                    throw new AppException(StatusCode.BAD_REQUEST, 
                        message("file.category.documents.only"));
                }
                maxSize = fileConfig.getMaxDocumentSize();
                if (file.getSize() > maxSize) {
                    throw new AppException(StatusCode.BAD_REQUEST, 
                        String.format(message("file.size.exceeded.category"), formatFileSize(maxSize)));
                }
                break;
                
            case "archives":
                if (!isArchiveFile(file)) {
                    throw new AppException(StatusCode.BAD_REQUEST, 
                        message("file.category.archives.only"));
                }
                maxSize = fileConfig.getMaxArchiveSize();
                if (file.getSize() > maxSize) {
                    throw new AppException(StatusCode.BAD_REQUEST, 
                        String.format(message("file.size.exceeded.category"), formatFileSize(maxSize)));
                }
                break;
                
            default:
                // Other categories: use default max size
                maxSize = fileConfig.getMaxDefaultSize();
                if (file.getSize() > maxSize) {
                    throw new AppException(StatusCode.BAD_REQUEST, 
                        message("file.size.exceeded"));
                }
                break;
        }
    }
    
    /**
     * Format file size to human readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}