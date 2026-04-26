package com.infinite.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderChildrenResponse {
    private String folderName;
    private String folderPath;
    private List<FileResourceDto> children;
}
