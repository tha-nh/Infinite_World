package com.infinite.file.service;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.file.dto.FileResourceDto;
import com.infinite.file.dto.FolderChildrenResponse;
import com.infinite.file.dto.TreeNode;
import com.infinite.file.dto.TreeSearchRequest;
import com.infinite.file.entity.FileResource;

import java.util.List;

public interface FileResourceService {
    
    /**
     * Resolve unique filename by checking duplicates and adding suffix if needed
     * This should be called BEFORE uploading to MinIO
     */
    String resolveUniqueFileName(String originalFileName, String parentPath);
    
    FileResource saveFileMetadata(String objectPath, String contentType, Long fileSize, String userId);
    
    void deleteFileMetadata(String objectPath);
    
    ApiResponse<String> getFileUrlByName(String name, String path1, String path2, String path3, String path4, String path5, String path6, String path7, String path8, String path9, String path10);
    
    ApiResponse<List<FolderChildrenResponse>> getFolderChildren(String folderName, String parentPath);
    
    ApiResponse<List<TreeNode>> searchTree(TreeSearchRequest request);
    
    FileResource findByObjectPath(String objectPath);
}
