package com.infinite.grpc.service.file.impl;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.FileUploadResponse;
import com.infinite.file.grpc.*;
import com.infinite.grpc.service.file.FileServiceGrpc;
import com.infinite.grpc.util.GrpcUtils;
import com.infinite.grpc.util.GrpcMultipartFile;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.multipart.MultipartFile;

import static com.infinite.common.dto.response.Response.message;

/**
 * gRPC implementation for File Service
 * 
 * NOTE: Exception handling is centralized in GrpcExceptionTranslator interceptor
 * No need for manual try/catch blocks - exceptions are automatically mapped to gRPC status
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
@ConditionalOnBean(name = "fileServiceGrpc")
public class FileServiceGrpcImpl extends FileServiceRpcGrpc.FileServiceRpcImplBase {

    private final FileServiceGrpc fileServiceGrpc;

    @Override
    public void uploadFile(UploadFileRequest request, StreamObserver<UploadFileResponse> responseObserver) {
        // Convert gRPC request to MultipartFile
        MultipartFile multipartFile = new GrpcMultipartFile(
                "file",
                request.getOriginalFilename(),
                request.getContentType(),
                request.getFileData().toByteArray()
        );

        // Call service - exceptions handled by GrpcExceptionTranslator
        ApiResponse<FileUploadResponse> result = fileServiceGrpc.uploadFile(
                multipartFile, 
                request.getCategory(), 
                request.getUserId()
        );

        // Build gRPC response
        UploadFileResponse.Builder responseBuilder = UploadFileResponse.newBuilder()
                .setCode(result.getCode())
                .setMessage(result.getMessage());

        if (result.getResult() != null) {
            FileUploadResponse uploadResult = result.getResult();
            FileInfo fileInfo = FileInfo.newBuilder()
                    .setFileName(uploadResult.getFileName())
                    .setOriginalFileName(uploadResult.getOriginalFileName())
                    .setFileUrl(uploadResult.getFileUrl())
                    .setRelativeUrl(uploadResult.getRelativeUrl() != null ? uploadResult.getRelativeUrl() : "")
                    .setFileType(uploadResult.getFileType())
                    .setFileSize(uploadResult.getFileSize())
                    .setCategory(uploadResult.getCategory())
                    .setUserId(request.getUserId())
                    .setCreatedAt(GrpcUtils.getCurrentDateTime())
                    .setUpdatedAt(GrpcUtils.getCurrentDateTime())
                    .build();
            
            responseBuilder.setFileInfo(fileInfo);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getFileUrl(GetFileUrlRequest request, StreamObserver<GetFileUrlResponse> responseObserver) {
        String[] pathParts = request.getFilePath().split("/");
        if (pathParts.length < 2) {
            GetFileUrlResponse errorResponse = GetFileUrlResponse.newBuilder()
                    .setCode(StatusCode.BAD_REQUEST.getCode())
                    .setMessage(message("grpc.file.path.invalid"))
                    .build();
            
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
            return;
        }

        String category = pathParts[0];
        String fileName = pathParts[1];
        
        // Call service - exceptions handled by GrpcExceptionTranslator
        String fileUrl = fileServiceGrpc.getFileUrl(fileName, category);

        GetFileUrlResponse response = GetFileUrlResponse.newBuilder()
                .setCode(StatusCode.SUCCESS.getCode())
                .setMessage(message("grpc.file.url.retrieved"))
                .setFileUrl(fileUrl)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteFile(DeleteFileRequest request, StreamObserver<DeleteFileResponse> responseObserver) {
        String[] pathParts = request.getFilePath().split("/");
        if (pathParts.length < 2) {
            DeleteFileResponse errorResponse = DeleteFileResponse.newBuilder()
                    .setCode(StatusCode.BAD_REQUEST.getCode())
                    .setMessage(message("grpc.file.path.invalid"))
                    .build();
            
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
            return;
        }

        String category = pathParts[0];
        String fileName = pathParts[1];
        
        // Call service - exceptions handled by GrpcExceptionTranslator
        ApiResponse<Object> result = fileServiceGrpc.deleteFile(fileName, category);

        DeleteFileResponse response = DeleteFileResponse.newBuilder()
                .setCode(result.getCode())
                .setMessage(result.getMessage())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getFileInfo(GetFileInfoRequest request, StreamObserver<GetFileInfoResponse> responseObserver) {
        String[] pathParts = request.getFilePath().split("/");
        if (pathParts.length < 2) {
            GetFileInfoResponse errorResponse = GetFileInfoResponse.newBuilder()
                    .setCode(StatusCode.BAD_REQUEST.getCode())
                    .setMessage(message("grpc.file.path.invalid"))
                    .build();
            
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
            return;
        }

        String category = pathParts[0];
        String fileName = pathParts[1];
        
        // Call service - exceptions handled by GrpcExceptionTranslator
        String fileUrl = fileServiceGrpc.getFileUrl(fileName, category);

        FileInfo fileInfo = FileInfo.newBuilder()
                .setFileName(fileName)
                .setOriginalFileName(fileName)
                .setFileUrl(fileUrl)
                .setCategory(category)
                .setCreatedAt(GrpcUtils.getCurrentDateTime())
                .setUpdatedAt(GrpcUtils.getCurrentDateTime())
                .build();

        GetFileInfoResponse response = GetFileInfoResponse.newBuilder()
                .setCode(StatusCode.SUCCESS.getCode())
                .setMessage(message("grpc.file.info.retrieved"))
                .setFileInfo(fileInfo)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listFiles(ListFilesRequest request, StreamObserver<ListFilesResponse> responseObserver) {
        // Placeholder - implement when needed
        ListFilesResponse response = ListFilesResponse.newBuilder()
                .setCode(StatusCode.INTERNAL_ERROR.getCode())
                .setMessage(message("grpc.file.list.not.implemented"))
                .setTotalElements(0)
                .setTotalPages(0)
                .setCurrentPage(request.getPage())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}