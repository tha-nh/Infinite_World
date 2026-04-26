package com.infinite.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeNode {
    private String name;
    private String type;
    private String path;
    private String url;
    private Long fileSize;
    private String contentType;
    private String extension;
    
    @Builder.Default
    private List<TreeNode> children = new ArrayList<>();
    
    public void addChild(TreeNode child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }
}
