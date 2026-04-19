package com.infinite.file.service;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    
    ApiResponse<FileUploadResponse> uploadFile(MultipartFile file, String category, String userId);
    
    ApiResponse<Object> deleteFile(String fileName, String category);
    
    String getFileUrl(String fileName, String category);
    
    boolean isValidFile(MultipartFile file);
    
    boolean isImageFile(MultipartFile file);
    
    boolean isVideoFile(MultipartFile file);
    
    boolean isDocumentFile(MultipartFile file);
}