package com.infinite.file.util;

import com.infinite.file.dto.TreeNode;
import com.infinite.file.entity.FileResource;

import java.util.*;
import java.util.stream.Collectors;

public class TreeBuilder {
    
    public static List<TreeNode> buildTree(List<FileResource> resources) {
        if (resources == null || resources.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Sort by path depth to ensure parents are processed before children
        List<FileResource> sorted = resources.stream()
                .sorted(Comparator.comparing(FileResource::getPathDepth))
                .collect(Collectors.toList());
        
        Map<String, TreeNode> nodeMap = new HashMap<>();
        List<TreeNode> roots = new ArrayList<>();
        
        for (FileResource resource : sorted) {
            TreeNode node = convertToTreeNode(resource);
            nodeMap.put(resource.getObjectPath(), node);
            
            if (resource.getParentPath() == null || resource.getParentPath().isEmpty()) {
                // Root node
                roots.add(node);
            } else {
                // Find parent and add as child
                TreeNode parent = nodeMap.get(resource.getParentPath());
                if (parent != null) {
                    parent.addChild(node);
                } else {
                    // Parent not found, treat as root
                    roots.add(node);
                }
            }
        }
        
        return roots;
    }
    
    private static TreeNode convertToTreeNode(FileResource resource) {
        return TreeNode.builder()
                .name(resource.getName())
                .type(resource.getResourceType())
                .path(resource.getObjectPath())
                .url(resource.getUrl())
                .fileSize(resource.getFileSize())
                .contentType(resource.getContentType())
                .extension(resource.getExtension())
                .children(new ArrayList<>())
                .build();
    }
}
