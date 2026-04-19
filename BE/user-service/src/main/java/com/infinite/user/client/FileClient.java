package com.infinite.user.client;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileClient {
    
    /**
     * Upload file async - không chờ kết quả, trả về ngay
     */
    void uploadFileAsync(MultipartFile file, String category, String userId, FileUploadCallback callback);
    
    /**
     * Upload file sync - chờ kết quả
     */
    ApiResponse<FileUploadResponse> uploadFile(MultipartFile file, String category, String userId);
    
    /**
     * Get file URL - cached để tránh gọi lại
     */
    String getFileUrl(String fileName, String category);
    
    /**
     * Delete file async
     */
    void deleteFileAsync(String fileName, String category);
    
    /**
     * Callback interface for async operations
     */
    interface FileUploadCallback {
        void onSuccess(FileUploadResponse response);
        void onError(String error);
    }
}