package com.infinite.grpc.client.file;

import com.google.protobuf.ByteString;
import com.infinite.common.constant.StatusCode;
import com.infinite.file.grpc.*;
import com.infinite.grpc.exception.GrpcClientException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * Wrapper client for File Service gRPC
 * Hides protobuf complexity from consumers
 * 
 * Usage:
 * <pre>
 * {@code
 * @Autowired
 * private FileGrpcClient fileClient;
 * 
 * String url = fileClient.getFileUrl("images", "test.jpg");
 * }
 * </pre>
 */
@Slf4j
@Component
public class FileGrpcClient {

    @GrpcClient("file-service")
    private FileServiceRpcGrpc.FileServiceRpcBlockingStub stub;

    /**
     * Upload file to file service
     * 
     * @param fileData File content as bytes
     * @param originalFilename Original filename
     * @param category File category (e.g., "images", "documents")
     * @param userId User ID
     * @param contentType MIME type
     * @return FileInfo with upload result
     */
    public FileInfo uploadFile(byte[] fileData, String originalFilename, String category, 
                               String userId, String contentType) {
        try {
            UploadFileRequest request = UploadFileRequest.newBuilder()
                    .setFileData(ByteString.copyFrom(fileData))
                    .setOriginalFilename(originalFilename)
                    .setCategory(category)
                    .setUserId(userId)
                    .setContentType(contentType)
                    .build();
            
            UploadFileResponse response = stub.uploadFile(request);
            
            if (response.getCode() != StatusCode.SUCCESS.getCode()) {
                throw new GrpcClientException("file-service", response.getCode(), response.getMessage());
            }
            
            return response.getFileInfo();
        } catch (io.grpc.StatusRuntimeException e) {
            log.error("gRPC error: status={}, description={}", e.getStatus(), e.getMessage(), e);
            throw new GrpcClientException("file-service", StatusCode.INTERNAL_ERROR.getCode(), 
                "gRPC call failed: " + e.getStatus().getDescription());
        }
    }

    /**
     * Get file URL
     * 
     * @param filePath File path in format "category/filename"
     * @return File URL
     */
    public String getFileUrl(String filePath) {
        GetFileUrlRequest request = GetFileUrlRequest.newBuilder()
                .setFilePath(filePath)
                .build();
        
        GetFileUrlResponse response = stub.getFileUrl(request);
        
        if (response.getCode() != StatusCode.SUCCESS.getCode()) {
            throw new GrpcClientException("file-service", response.getCode(), response.getMessage());
        }
        
        return response.getFileUrl();
    }

    /**
     * Get file URL by category and filename
     * 
     * @param category File category
     * @param fileName File name
     * @return File URL
     */
    public String getFileUrl(String category, String fileName) {
        return getFileUrl(category + "/" + fileName);
    }

    /**
     * Delete file
     * 
     * @param filePath File path in format "category/filename"
     */
    public void deleteFile(String filePath) {
        DeleteFileRequest request = DeleteFileRequest.newBuilder()
                .setFilePath(filePath)
                .build();
        
        DeleteFileResponse response = stub.deleteFile(request);
        
        if (response.getCode() != StatusCode.SUCCESS.getCode()) {
            throw new GrpcClientException("file-service", response.getCode(), response.getMessage());
        }
    }

    /**
     * Delete file by category and filename
     * 
     * @param category File category
     * @param fileName File name
     */
    public void deleteFile(String category, String fileName) {
        deleteFile(category + "/" + fileName);
    }

    /**
     * Get file info
     * 
     * @param filePath File path in format "category/filename"
     * @return FileInfo
     */
    public FileInfo getFileInfo(String filePath) {
        GetFileInfoRequest request = GetFileInfoRequest.newBuilder()
                .setFilePath(filePath)
                .build();
        
        GetFileInfoResponse response = stub.getFileInfo(request);
        
        if (response.getCode() != StatusCode.SUCCESS.getCode()) {
            throw new GrpcClientException("file-service", response.getCode(), response.getMessage());
        }
        
        return response.getFileInfo();
    }

    /**
     * Get file info by category and filename
     * 
     * @param category File category
     * @param fileName File name
     * @return FileInfo
     */
    public FileInfo getFileInfo(String category, String fileName) {
        return getFileInfo(category + "/" + fileName);
    }
}
