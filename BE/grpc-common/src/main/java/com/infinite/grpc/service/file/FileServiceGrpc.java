package com.infinite.grpc.service.file;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for File Service to be used by gRPC
 * Services implementing this interface can be used by gRPC layer
 */
public interface FileServiceGrpc {
    
    ApiResponse<FileUploadResponse> uploadFile(MultipartFile file, String category, String userId);
    
    ApiResponse<Object> deleteFile(String fileName, String category);
    
    String getFileUrl(String fileName, String category);
}