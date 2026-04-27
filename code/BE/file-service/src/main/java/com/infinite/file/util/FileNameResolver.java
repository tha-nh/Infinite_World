package com.infinite.file.util;

import com.infinite.file.repository.FileResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileNameResolver {
    
    private final FileResourceRepository fileResourceRepository;
    
    /**
     * Resolve unique name by adding (1), (2), ... suffix if name already exists in parent path
     * 
     * @param baseName Original name
     * @param parentPath Parent path to check uniqueness
     * @return Unique name
     */
    public String resolveUniqueName(String baseName, String parentPath) {
        if (baseName == null || baseName.isEmpty()) {
            return baseName;
        }
        
        String name = baseName;
        int counter = 1;
        
        while (fileResourceRepository.existsByParentPathAndName(parentPath, name)) {
            String nameWithoutExt = getNameWithoutExtension(baseName);
            String ext = getExtension(baseName);
            
            name = nameWithoutExt + "(" + counter + ")" + (ext.isEmpty() ? "" : "." + ext);
            counter++;
            
            // Safety check to prevent infinite loop
            if (counter > 1000) {
                throw new IllegalStateException("Too many duplicate names for: " + baseName);
            }
        }
        
        return name;
    }
    
    private String getNameWithoutExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return fileName;
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        return fileName.substring(0, lastDotIndex);
    }
    
    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf(".");
        return fileName.substring(lastDotIndex + 1);
    }
}
