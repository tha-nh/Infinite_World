package com.infinite.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private String fileName;
    private String originalFileName;
    private String fileUrl;  // Full URL with host/IP for client response
    private String relativeUrl;  // Relative URL (encoded, without host/IP) for service storage
    private String fileType;
    private long fileSize;
    private String category;
}