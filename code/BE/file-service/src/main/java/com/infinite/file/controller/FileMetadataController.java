package com.infinite.file.controller;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.file.dto.FolderChildrenResponse;
import com.infinite.file.dto.TreeNode;
import com.infinite.file.dto.TreeSearchRequest;
import com.infinite.file.service.FileResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/files")
@RequiredArgsConstructor
@Tag(name = "File Metadata Management", description = "APIs for file metadata search and tree operations")
public class FileMetadataController {
    
    private final FileResourceService fileResourceService;
    
    @Operation(
        summary = "Get file URL by name",
        description = "Get file URL by name with optional path filters. If multiple files found, provide more path filters."
    )
    @GetMapping("/url")
    public ApiResponse<String> getFileUrlByName(
            @Parameter(description = "File name", required = true)
            @RequestParam String name,
            @Parameter(description = "Path level 1 filter")
            @RequestParam(required = false) String path1,
            @Parameter(description = "Path level 2 filter")
            @RequestParam(required = false) String path2,
            @Parameter(description = "Path level 3 filter")
            @RequestParam(required = false) String path3,
            @Parameter(description = "Path level 4 filter")
            @RequestParam(required = false) String path4,
            @Parameter(description = "Path level 5 filter")
            @RequestParam(required = false) String path5,
            @Parameter(description = "Path level 6 filter")
            @RequestParam(required = false) String path6,
            @Parameter(description = "Path level 7 filter")
            @RequestParam(required = false) String path7,
            @Parameter(description = "Path level 8 filter")
            @RequestParam(required = false) String path8,
            @Parameter(description = "Path level 9 filter")
            @RequestParam(required = false) String path9,
            @Parameter(description = "Path level 10 filter")
            @RequestParam(required = false) String path10) {
        return fileResourceService.getFileUrlByName(name, path1, path2, path3, path4, path5, path6, path7, path8, path9, path10);
    }
    
    @Operation(
        summary = "Get folder children",
        description = "Get list of files and folders inside a folder. Use parentPath for exact match or folderName for search."
    )
    @GetMapping("/folder/children")
    public ApiResponse<List<FolderChildrenResponse>> getFolderChildren(
            @Parameter(description = "Folder name (may return multiple folders if name is not unique)")
            @RequestParam(required = false) String folderName,
            @Parameter(description = "Parent path (exact match, recommended)")
            @RequestParam(required = false) String parentPath) {
        return fileResourceService.getFolderChildren(folderName, parentPath);
    }
    
    @Operation(
        summary = "Search file tree",
        description = "Search files and folders with path filters and return tree structure"
    )
    @PostMapping("/tree/search")
    public ApiResponse<List<TreeNode>> searchTree(@RequestBody TreeSearchRequest request) {
        return fileResourceService.searchTree(request);
    }
}
