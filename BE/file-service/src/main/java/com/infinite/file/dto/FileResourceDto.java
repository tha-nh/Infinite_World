package com.infinite.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResourceDto {
    private Long id;
    private String resourceType;
    private String name;
    private String url;
    private String objectPath;
    private String parentPath;
    private Long fileSize;
    private String contentType;
    private String extension;
}
