package com.infinite.file.service.impl;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.exception.AppException;
import com.infinite.file.config.FileConfig;
import com.infinite.common.dto.response.FileUploadResponse;
import com.infinite.file.service.FileService;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

import static com.infinite.common.constant.StatusCode.SUCCESS;
import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {
    
    private final FileConfig fileConfig;
    private final MinioClient minioClient;
    
    @Override
    public ApiResponse<FileUploadResponse> uploadFile(MultipartFile file, String category, String userId) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new AppException(StatusCode.BAD_REQUEST, "File is empty");
            }
            
            if (!isValidFile(file)) {
                String fileExtension = getFileExtension(file.getOriginalFilename());
                String allowedTypesStr = String.join(", ", fileConfig.getAllowedTypes());
                throw new AppException(StatusCode.BAD_REQUEST, 
                    String.format("Invalid file type: .%s. Allowed types: %s", fileExtension, allowedTypesStr));
            }
            
            if (file.getSize() > fileConfig.getMaxFileSize()) {
                throw new AppException(StatusCode.BAD_REQUEST, "File size exceeds maximum limit");
            }
            
            // Ensure bucket exists
            ensureBucketExists();
            
            // Generate unique filename
            String fileName = generateFileName(file.getOriginalFilename());
            String objectName = category + "/" + fileName;
            
            // Upload to MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(fileConfig.getBucketName())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            // Build file URL
            String fileUrl = getFileUrl(fileName, category);
            
            FileUploadResponse response = FileUploadResponse.builder()
                    .fileName(fileName)
                    .originalFileName(file.getOriginalFilename())
                    .fileUrl(fileUrl)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .category(category)
                    .build();
            
            log.info("File uploaded successfully to MinIO: {}", objectName);
            
            return ApiResponse.<FileUploadResponse>builder()
                    .code(code(SUCCESS))
                    .message(message("file.upload.success"))
                    .result(response)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error uploading file to MinIO: ", e);
            throw new AppException(StatusCode.INTERNAL_ERROR, "Failed to upload file: " + e.getMessage());
        }
    }
    
    @Override
    public ApiResponse<Object> deleteFile(String fileName, String category) {
        try {
            String objectName = category + "/" + fileName;
            
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(fileConfig.getBucketName())
                    .object(objectName)
                    .build()
            );
            
            log.info("File deleted successfully from MinIO: {}", objectName);
            
            return ApiResponse.builder()
                    .code(code(SUCCESS))
                    .message(message("file.delete.success"))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error deleting file from MinIO: ", e);
            throw new AppException(StatusCode.INTERNAL_ERROR, "Failed to delete file: " + e.getMessage());
        }
    }
    
    @Override
    public String getFileUrl(String fileName, String category) {
        try {
            String objectName = category + "/" + fileName;
            
            // Generate presigned URL for file access (valid for 7 days)
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.GET)
                    .bucket(fileConfig.getBucketName())
                    .object(objectName)
                    .expiry(7 * 24 * 60 * 60) // 7 days
                    .build()
            );
            
        } catch (Exception e) {
            log.error("Error generating file URL: ", e);
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
        String[] imageTypes = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
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
        String[] documentTypes = {"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"};
        String fileExtension = getFileExtension(file.getOriginalFilename());
        
        return Arrays.stream(documentTypes)
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
            log.info("Created bucket: {}", fileConfig.getBucketName());
        }
    }
    
    private String generateFileName(String originalFileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(originalFileName);
        return timestamp + "_" + uuid + "." + extension;
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}