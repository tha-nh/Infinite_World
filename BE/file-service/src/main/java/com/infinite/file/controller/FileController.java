package com.infinite.file.controller;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.dto.response.FileUploadResponse;
import com.infinite.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/api/files")
@RequiredArgsConstructor
@Tag(name = "File Management", description = "APIs for file upload and management with MinIO")
public class FileController {
    
    private final FileService fileService;
    
    @Operation(summary = "Upload file", description = "Upload file (image, video, document) to MinIO")
    @PostMapping("/upload")
    public ApiResponse<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "avatar") String category,
            @RequestParam(value = "userId", required = false) String userId) {
        return fileService.uploadFile(file, category, userId);
    }
    
    @Operation(summary = "Get file URL", description = "Get presigned URL for file access")
    @GetMapping("/{category}/{fileName}")
    public String getFileUrl(
            @PathVariable String category,
            @PathVariable String fileName) {
        return fileService.getFileUrl(fileName, category);
    }
    
    @Operation(summary = "Delete file", description = "Delete file from MinIO")
    @DeleteMapping("/{category}/{fileName}")
    public ApiResponse<Object> deleteFile(
            @PathVariable String category,
            @PathVariable String fileName) {
        return fileService.deleteFile(fileName, category);
    }
}