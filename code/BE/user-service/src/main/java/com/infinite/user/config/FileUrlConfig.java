package com.infinite.user.config;

import com.infinite.common.util.FileUrlBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileUrlConfig {
    
    @Value("${file.public.base-url:http://localhost:9000}")
    private String publicBaseUrl;
    
    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }
    
    /**
     * Build full URL by adding host/IP to stored relative URL
     * Stored relative URL is already encoded, just need to ghép host
     * @param storedRelativeUrl Stored relative URL in DB (already encoded)
     * @return Full URL with host/IP
     */
    public String buildFullUrl(String storedRelativeUrl) {
        if (storedRelativeUrl == null || storedRelativeUrl.isEmpty()) {
            return null;
        }
        // Use common utility to ghép host vào stored relative URL (already encoded)
        return FileUrlBuilder.buildFullUrl(publicBaseUrl, storedRelativeUrl);
    }
}
