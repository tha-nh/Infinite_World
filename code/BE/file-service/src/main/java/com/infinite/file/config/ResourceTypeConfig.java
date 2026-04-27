package com.infinite.file.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "file.resource")
@Getter
public class ResourceTypeConfig {
    
    private Map<String, List<String>> types = new HashMap<>();
    
    public void setTypes(Map<String, List<String>> types) {
        this.types = types;
    }
    
    public String determineResourceType(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "FILE";
        }
        
        String extLower = extension.toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : types.entrySet()) {
            if (entry.getValue().contains(extLower)) {
                return entry.getKey().toUpperCase();
            }
        }
        
        return "FILE";
    }
    
    public boolean isValidResourceType(String type) {
        if (type == null) {
            return false;
        }
        return types.containsKey(type.toLowerCase()) || "FILE".equalsIgnoreCase(type) || "FOLDER".equalsIgnoreCase(type);
    }
}
