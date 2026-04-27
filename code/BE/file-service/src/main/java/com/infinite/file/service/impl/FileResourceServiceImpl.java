package com.infinite.file.service.impl;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.exception.AppException;
import com.infinite.common.util.FileUrlBuilder;
import com.infinite.file.config.FileConfig;
import com.infinite.file.config.ResourceTypeConfig;
import com.infinite.file.dto.FileResourceDto;
import com.infinite.file.dto.FolderChildrenResponse;
import com.infinite.file.dto.TreeNode;
import com.infinite.file.dto.TreeSearchRequest;
import com.infinite.file.entity.FileResource;
import com.infinite.file.repository.FileResourceRepository;
import com.infinite.file.service.FileResourceService;
import com.infinite.file.util.FileNameResolver;
import com.infinite.file.util.PathParser;
import com.infinite.file.util.TreeBuilder;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.infinite.common.constant.StatusCode.SUCCESS;
import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileResourceServiceImpl implements FileResourceService {
    
    private final FileResourceRepository fileResourceRepository;
    private final MinioClient minioClient;
    private final FileConfig fileConfig;
    private final ResourceTypeConfig resourceTypeConfig;
    private final FileNameResolver fileNameResolver;
    
    @Override
    public String resolveUniqueFileName(String originalFileName, String parentPath) {
        return fileNameResolver.resolveUniqueName(originalFileName, parentPath);
    }
    
    @Override
    @Transactional
    public FileResource saveFileMetadata(String objectPath, String contentType, Long fileSize, String userId) {
        try {
            // Parse object path
            PathParser.PathInfo pathInfo = PathParser.parse(objectPath);
            
            // Extract extension from name
            String extension = extractExtension(pathInfo.getName());
            
            // Determine resource type from configuration
            String resourceType = resourceTypeConfig.determineResourceType(extension);
            
            // Build relative URL (without host/IP, with leading slash) for storage
            String relativeUrl = buildRelativeUrl(objectPath);
            
            // Build FileResource entity
            FileResource resource = FileResource.builder()
                    .resourceType(resourceType)
                    .name(pathInfo.getName())
                    .url(relativeUrl) // Lưu relative URL: /bucket/category/filename
                    .isUrlExpirable(false) // Hiện tại chỉ lưu permanent URL
                    .bucketName(pathInfo.getBucketName())
                    .objectPath(objectPath)
                    .parentPath(pathInfo.getParentPath())
                    .pathDepth(pathInfo.getPathDepth())
                    .path1(pathInfo.getPath1())
                    .path2(pathInfo.getPath2())
                    .path3(pathInfo.getPath3())
                    .path4(pathInfo.getPath4())
                    .path5(pathInfo.getPath5())
                    .path6(pathInfo.getPath6())
                    .path7(pathInfo.getPath7())
                    .path8(pathInfo.getPath8())
                    .path9(pathInfo.getPath9())
                    .path10(pathInfo.getPath10())
                    .extension(extension)
                    .contentType(contentType)
                    .fileSize(fileSize)
                    .createdBy(userId != null ? userId : "system")
                    .updatedBy(userId != null ? userId : "system")
                    .build();
            
            // Save to database
            FileResource saved = fileResourceRepository.save(resource);
            
            // Upsert parent folders (create if not exists, or partial update if exists)
            upsertParentFolders(pathInfo, userId);
            
            return saved;
            
        } catch (Exception e) {
            log.error("Error saving file metadata: ", e);
            throw new AppException(StatusCode.INTERNAL_ERROR, "Failed to save file metadata: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void deleteFileMetadata(String objectPath) {
        try {
            fileResourceRepository.findByObjectPath(objectPath)
                    .ifPresent(resource -> {
                        fileResourceRepository.delete(resource);
                    });
        } catch (Exception e) {
            log.error("Error deleting file metadata: ", e);
            throw new AppException(StatusCode.INTERNAL_ERROR, "Failed to delete file metadata: " + e.getMessage());
        }
    }
    
    @Override
    public ApiResponse<String> getFileUrlByName(String name, String path1, String path2, String path3, String path4, String path5, String path6, String path7, String path8, String path9, String path10) {
        try {
            List<FileResource> resources = new ArrayList<>();
            
            // Build query based on provided path filters
            if (path10 != null) {
                resources = fileResourceRepository.searchByPaths(path1, path2, path3, path4, path5, path6, path7, path8, path9, path10, name, null);
            } else if (path9 != null) {
                resources = fileResourceRepository.searchByPaths(path1, path2, path3, path4, path5, path6, path7, path8, path9, null, name, null);
            } else if (path8 != null) {
                resources = fileResourceRepository.searchByPaths(path1, path2, path3, path4, path5, path6, path7, path8, null, null, name, null);
            } else if (path7 != null) {
                resources = fileResourceRepository.searchByPaths(path1, path2, path3, path4, path5, path6, path7, null, null, null, name, null);
            } else if (path6 != null) {
                resources = fileResourceRepository.searchByPaths(path1, path2, path3, path4, path5, path6, null, null, null, null, name, null);
            } else if (path5 != null) {
                resources = fileResourceRepository.searchByPaths(path1, path2, path3, path4, path5, null, null, null, null, null, name, null);
            } else if (path4 != null) {
                resources = fileResourceRepository.searchByPaths(path1, path2, path3, path4, null, null, null, null, null, null, name, null);
            } else if (path3 != null) {
                resources = fileResourceRepository.searchByPaths(path1, path2, path3, null, null, null, null, null, null, null, name, null);
            } else if (path2 != null) {
                resources = fileResourceRepository.findByNameAndPath1AndPath2(name, path1, path2);
            } else if (path1 != null) {
                resources = fileResourceRepository.findByNameAndPath1(name, path1);
            } else {
                resources = fileResourceRepository.findByName(name);
            }
            
            if (resources.isEmpty()) {
                throw new AppException(StatusCode.FILE_NOT_EXISTED, message("file.url.not.found"));
            }
            
            if (resources.size() > 1) {
                throw new AppException(StatusCode.BAD_REQUEST, message("file.url.multiple.found"));
            }
            
            FileResource resource = resources.get(0);
            
            // Build full URL lúc response (ghép host vào relative URL)
            String fileUrl = buildFullUrl(resource.getUrl());
            
            return ApiResponse.<String>builder()
                    .code(code(SUCCESS))
                    .message(message("file.url.retrieved"))
                    .result(fileUrl)
                    .build();
                    
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting file URL by name: ", e);
            throw new AppException(StatusCode.INTERNAL_ERROR, "Failed to get file URL: " + e.getMessage());
        }
    }
    
    @Override
    public ApiResponse<List<FolderChildrenResponse>> getFolderChildren(String folderName, String parentPath) {
        try {
            List<FolderChildrenResponse> responseList = new ArrayList<>();
            
            if (parentPath != null && !parentPath.isEmpty()) {
                // Query by exact parent path
                List<FileResource> children = fileResourceRepository.findByParentPath(parentPath);
                
                // Sort: folders first, then files, then by name
                children.sort(Comparator
                        .comparing((FileResource r) -> "FOLDER".equals(r.getResourceType()) ? 0 : 1)
                        .thenComparing(FileResource::getName));
                
                // Build URLs lúc response cho files
                for (FileResource child : children) {
                    if (!"FOLDER".equals(child.getResourceType())) {
                        String fileUrl = buildFullUrl(child.getUrl());
                        child.setUrl(fileUrl); // Set tạm thời cho response, không save DB
                    }
                }
                
                FolderChildrenResponse response = FolderChildrenResponse.builder()
                        .folderName(extractFolderName(parentPath))
                        .folderPath(parentPath)
                        .children(children.stream()
                                .map(this::convertToDto)
                                .collect(Collectors.toList()))
                        .build();
                
                responseList.add(response);
                
            } else if (folderName != null && !folderName.isEmpty()) {
                // Query by folder name - may return multiple folders
                List<FileResource> folders = fileResourceRepository.findByNameOrderByObjectPath(folderName);
                
                for (FileResource folder : folders) {
                    if ("FOLDER".equals(folder.getResourceType())) {
                        List<FileResource> children = fileResourceRepository.findByParentPath(folder.getObjectPath());
                        
                        // Sort children
                        children.sort(Comparator
                                .comparing((FileResource r) -> "FOLDER".equals(r.getResourceType()) ? 0 : 1)
                                .thenComparing(FileResource::getName));
                        
                        // Build URLs lúc response
                        for (FileResource child : children) {
                            if (!"FOLDER".equals(child.getResourceType())) {
                                String fileUrl = buildFullUrl(child.getUrl());
                                child.setUrl(fileUrl); // Set tạm thời cho response, không save DB
                            }
                        }
                        
                        FolderChildrenResponse response = FolderChildrenResponse.builder()
                                .folderName(folder.getName())
                                .folderPath(folder.getObjectPath())
                                .children(children.stream()
                                        .map(this::convertToDto)
                                        .collect(Collectors.toList()))
                                .build();
                        
                        responseList.add(response);
                    }
                }
            }
            
            if (responseList.isEmpty()) {
                throw new AppException(StatusCode.FILE_NOT_EXISTED, message("file.folder.not.found"));
            }
            
            return ApiResponse.<List<FolderChildrenResponse>>builder()
                    .code(code(SUCCESS))
                    .message(message("file.folder.children.success"))
                    .result(responseList)
                    .build();
                    
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting folder children: ", e);
            throw new AppException(StatusCode.INTERNAL_ERROR, "Failed to get folder children: " + e.getMessage());
        }
    }
    
    @Override
    public ApiResponse<List<TreeNode>> searchTree(TreeSearchRequest request) {
        try {
            List<FileResource> resources = fileResourceRepository.searchByPaths(
                    request.getPath1(),
                    request.getPath2(),
                    request.getPath3(),
                    request.getPath4(),
                    request.getPath5(),
                    request.getPath6(),
                    request.getPath7(),
                    request.getPath8(),
                    request.getPath9(),
                    request.getPath10(),
                    request.getName(),
                    request.getResourceType()
            );
            
            // Build URLs lúc response cho files
            for (FileResource resource : resources) {
                if (!"FOLDER".equals(resource.getResourceType())) {
                    String fileUrl = buildFullUrl(resource.getUrl());
                    resource.setUrl(fileUrl); // Set tạm thời cho response, không save DB
                }
            }
            
            // Build tree
            List<TreeNode> tree = TreeBuilder.buildTree(resources);
            
            return ApiResponse.<List<TreeNode>>builder()
                    .code(code(SUCCESS))
                    .message(message("file.tree.search.success"))
                    .result(tree)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error searching tree: ", e);
            throw new AppException(StatusCode.INTERNAL_ERROR, "Failed to search tree: " + e.getMessage());
        }
    }
    
    @Override
    public FileResource findByObjectPath(String objectPath) {
        return fileResourceRepository.findByObjectPath(objectPath)
                .orElseThrow(() -> new AppException(StatusCode.FILE_NOT_EXISTED, message("file.metadata.not.found")));
    }
    
    private String generatePresignedUrl(String objectPath) {
        try {
            // Extract object key from object path (remove bucket prefix)
            String objectKey = objectPath.substring(fileConfig.getBucketName().length() + 1);
            
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(io.minio.http.Method.GET)
                            .bucket(fileConfig.getBucketName())
                            .object(objectKey)
                            .expiry(7 * 24 * 60 * 60) // 7 days
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned URL: ", e);
            return null;
        }
    }
    
    /**
     * Build relative URL (without host/IP, with leading slash) for storage
     * Path is encoded before storage so it can be used directly
     * @param objectPath Full object path (bucket/category/filename)
     * @return Encoded relative URL ready for storage (/bucket/category/filename - encoded)
     */
    private String buildRelativeUrl(String objectPath) {
        // Encode and normalize path for storage
        return FileUrlBuilder.encodeAndNormalizeForStorage(objectPath);
    }
    
    /**
     * Build full URL by adding host/IP to stored relative URL
     * Stored relative URL is already encoded, just need to ghép host
     * @param storedRelativeUrl Stored relative URL in DB (already encoded, e.g., /bucket/category/t%E1%BA%A3i%20xu%E1%BB%91ng%20%283%29.jpg)
     * @return Full URL with host/IP
     */
    private String buildFullUrl(String storedRelativeUrl) {
        try {
            if (storedRelativeUrl == null || storedRelativeUrl.isEmpty()) {
                return null;
            }
            // Use common utility to ghép host vào stored relative URL (already encoded)
            return FileUrlBuilder.buildFullUrl(fileConfig.getPublicBaseUrl(), storedRelativeUrl);
        } catch (Exception e) {
            log.error("Error building full URL: ", e);
            return null;
        }
    }
    
    /**
     * Build file URL based on urlType
     * @param objectPath Full object path (bucket/category/filename)
     * @param urlType "presigned" or "permanent"
     * @return Built URL
     */
    private String buildFileUrl(String objectPath, String urlType) {
        try {
            if ("permanent".equals(urlType)) {
                // Encode object path for URL
                String encodedRelativePath = FileUrlBuilder.encodeAndNormalizeForStorage(objectPath);
                // Build full URL from encoded relative path
                return FileUrlBuilder.buildFullUrl(fileConfig.getPublicBaseUrl(), encodedRelativePath);
            } else {
                // Default: presigned URL
                return generatePresignedUrl(objectPath);
            }
        } catch (Exception e) {
            log.error("Error building file URL: ", e);
            return generatePresignedUrl(objectPath); // Fallback to presigned
        }
    }
    
    private void upsertParentFolders(PathParser.PathInfo pathInfo, String userId) {
        if (pathInfo.getParentPath() == null || pathInfo.getParentPath().isEmpty()) {
            return;
        }
        
        try {
            // Build parent path segments
            String[] segments = pathInfo.getParentPath().split("/");
            StringBuilder currentPath = new StringBuilder();
            
            for (int i = 0; i < segments.length; i++) {
                if (i > 0) {
                    currentPath.append("/");
                }
                currentPath.append(segments[i]);
                
                String folderPath = currentPath.toString();
                String folderName = segments[i];
                String folderParentPath = i > 0 ? folderPath.substring(0, folderPath.lastIndexOf("/")) : null;
                
                // Check if folder already exists
                Optional<FileResource> existingFolder = fileResourceRepository.findByObjectPath(folderPath);
                
                if (existingFolder.isEmpty()) {
                    // Create new folder node
                    FileResource folder = FileResource.builder()
                            .resourceType("FOLDER")
                            .name(folderName)
                            .bucketName(pathInfo.getBucketName())
                            .objectPath(folderPath)
                            .parentPath(folderParentPath)
                            .pathDepth(i)
                            .path1(i >= 0 ? segments[0] : null)
                            .path2(i >= 1 ? segments[1] : null)
                            .path3(i >= 2 ? segments[2] : null)
                            .path4(i >= 3 ? segments[3] : null)
                            .path5(i >= 4 ? segments[4] : null)
                            .path6(i >= 5 ? segments[5] : null)
                            .path7(i >= 6 ? segments[6] : null)
                            .path8(i >= 7 ? segments[7] : null)
                            .path9(i >= 8 ? segments[8] : null)
                            .path10(i >= 9 ? segments[9] : null)
                            .isUrlExpirable(false)
                            .createdBy(userId != null ? userId : "system")
                            .updatedBy(userId != null ? userId : "system")
                            .build();
                    
                    fileResourceRepository.save(folder);
                } else {
                    // Partial update existing folder
                    FileResource folder = existingFolder.get();
                    boolean updated = false;
                    
                    // Update fields if they differ
                    if (!folderName.equals(folder.getName())) {
                        folder.setName(folderName);
                        updated = true;
                    }
                    
                    if (folderParentPath != null && !folderParentPath.equals(folder.getParentPath())) {
                        folder.setParentPath(folderParentPath);
                        updated = true;
                    }
                    
                    if (i != folder.getPathDepth()) {
                        folder.setPathDepth(i);
                        updated = true;
                    }
                    
                    // Update path fields if needed
                    if (i >= 0 && !segments[0].equals(folder.getPath1())) {
                        folder.setPath1(segments[0]);
                        updated = true;
                    }
                    if (i >= 1 && !segments[1].equals(folder.getPath2())) {
                        folder.setPath2(segments[1]);
                        updated = true;
                    }
                    if (i >= 2 && !segments[2].equals(folder.getPath3())) {
                        folder.setPath3(segments[2]);
                        updated = true;
                    }
                    if (i >= 3 && !segments[3].equals(folder.getPath4())) {
                        folder.setPath4(segments[3]);
                        updated = true;
                    }
                    if (i >= 4 && !segments[4].equals(folder.getPath5())) {
                        folder.setPath5(segments[4]);
                        updated = true;
                    }
                    if (i >= 5 && !segments[5].equals(folder.getPath6())) {
                        folder.setPath6(segments[5]);
                        updated = true;
                    }
                    if (i >= 6 && !segments[6].equals(folder.getPath7())) {
                        folder.setPath7(segments[6]);
                        updated = true;
                    }
                    if (i >= 7 && !segments[7].equals(folder.getPath8())) {
                        folder.setPath8(segments[7]);
                        updated = true;
                    }
                    if (i >= 8 && !segments[8].equals(folder.getPath9())) {
                        folder.setPath9(segments[8]);
                        updated = true;
                    }
                    if (i >= 9 && !segments[9].equals(folder.getPath10())) {
                        folder.setPath10(segments[9]);
                        updated = true;
                    }
                    
                    if (updated) {
                        folder.setUpdatedBy(userId != null ? userId : "system");
                        fileResourceRepository.save(folder);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error upserting parent folders: ", e);
            // Don't throw exception, just log warning
        }
    }
    
    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    private String extractFolderName(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        String[] segments = path.split("/");
        return segments[segments.length - 1];
    }
    
    private FileResourceDto convertToDto(FileResource resource) {
        return FileResourceDto.builder()
                .id(resource.getId())
                .resourceType(resource.getResourceType())
                .name(resource.getName())
                .url(resource.getUrl())
                .objectPath(resource.getObjectPath())
                .parentPath(resource.getParentPath())
                .fileSize(resource.getFileSize())
                .contentType(resource.getContentType())
                .extension(resource.getExtension())
                .build();
    }
}
