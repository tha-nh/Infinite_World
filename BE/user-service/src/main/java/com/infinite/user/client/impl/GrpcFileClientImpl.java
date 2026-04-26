package com.infinite.user.client.impl;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.FileUploadResponse;
import com.infinite.file.grpc.FileInfo;
import com.infinite.grpc.client.file.FileGrpcClient;
import com.infinite.grpc.exception.GrpcClientException;
import com.infinite.user.client.FileClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.infinite.common.dto.response.Response.message;

/**
 * gRPC implementation of FileClient
 * Adapter that converts between user-service business interface and gRPC client
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrpcFileClientImpl implements FileClient {

    private final FileGrpcClient fileGrpcClient;

    @Override
    @Async
    public void uploadFileAsync(MultipartFile file, String category, String userId, FileUploadCallback callback) {
        try {
            ApiResponse<FileUploadResponse> response = uploadFile(file, category, userId);
            if (response.getCode() == StatusCode.SUCCESS.getCode()) {
                callback.onSuccess(response.getResult());
            } else {
                callback.onError(response.getMessage());
            }
        } catch (Exception e) {
            log.error("Async upload failed");
            callback.onError(e.getMessage());
        }
    }

    @Override
    public ApiResponse<FileUploadResponse> uploadFile(MultipartFile file, String category, String userId) {
        try {
            // Convert MultipartFile to byte array
            byte[] fileData = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();

            // Call gRPC client
            FileInfo fileInfo = fileGrpcClient.uploadFile(
                    fileData,
                    originalFilename,
                    category,
                    userId,
                    contentType
            );

            // Convert FileInfo to FileUploadResponse
            FileUploadResponse uploadResponse = FileUploadResponse.builder()
                    .fileName(fileInfo.getFileName())
                    .originalFileName(fileInfo.getOriginalFileName())
                    .fileUrl(fileInfo.getFileUrl())
                    .fileType(fileInfo.getFileType())
                    .fileSize(fileInfo.getFileSize())
                    .category(fileInfo.getCategory())
                    .build();

            return ApiResponse.<FileUploadResponse>builder()
                    .code(StatusCode.SUCCESS.getCode())
                    .message(message("file.upload.success"))
                    .result(uploadResponse)
                    .build();

        } catch (IOException e) {
            log.error("Failed to read file: {}", e.getMessage());
            return ApiResponse.<FileUploadResponse>builder()
                    .code(StatusCode.FILE_NOT_READ.getCode())
                    .message(message("file.read.error"))
                    .build();

        } catch (GrpcClientException e) {
            return ApiResponse.<FileUploadResponse>builder()
                    .code(e.getBusinessCode())
                    .message(e.getBusinessMessage())
                    .build();

        } catch (Exception e) {
            log.error("Upload error: {}", e.getMessage(), e);
            return ApiResponse.<FileUploadResponse>builder()
                    .code(StatusCode.INTERNAL_ERROR.getCode())
                    .message(message("file.upload.error"))
                    .build();
        }
    }

    @Override
    public String getFileUrl(String fileName, String category) {
        try {
            return fileGrpcClient.getFileUrl(category, fileName);
        } catch (GrpcClientException e) {
            throw new RuntimeException(e.getBusinessMessage(), e);
        } catch (Exception e) {
            log.error("Get file URL error: {}", e.getMessage());
            throw new RuntimeException("Failed to get file URL", e);
        }
    }

    @Override
    @Async
    public void deleteFileAsync(String fileName, String category) {
        try {
            fileGrpcClient.deleteFile(category, fileName);
        } catch (Exception e) {
            log.error("Delete file failed: {}/{}", category, fileName);
        }
    }
}
