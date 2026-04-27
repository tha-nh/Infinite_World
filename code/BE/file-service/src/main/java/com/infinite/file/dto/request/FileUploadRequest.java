package com.infinite.file.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadRequest {
    private MultipartFile file;
    private String category; // "avatar", "document", etc.
    private String userId;
}