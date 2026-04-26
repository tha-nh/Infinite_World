package com.infinite.common.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for building and encoding file URLs
 * Provides common functionality for all services to build full URLs from base URL and relative paths
 * 
 * Key principles:
 * - Store raw file names in DB (no encoding)
 * - Encode only when building URL response
 * - Each path segment encoded separately, '/' separators preserved
 * - Reusable across all services (file-service, user-service, etc.)
 */
public class FileUrlBuilder {
    
    /**
     * Encode a single path segment (file or folder name)
     * Encodes special characters but preserves safe characters
     * 
     * @param segment Path segment to encode (e.g., "tải xuống (3).jpg")
     * @return Encoded segment (e.g., "t%E1%BA%A3i%20xu%E1%BB%91ng%20%283%29.jpg")
     */
    public static String encodePathSegment(String segment) {
        if (segment == null || segment.isEmpty()) {
            return segment;
        }
        
        try {
            // URLEncoder.encode encodes everything except unreserved characters
            // We use UTF-8 encoding for proper handling of Vietnamese and other characters
            String encoded = URLEncoder.encode(segment, StandardCharsets.UTF_8.toString());
            
            // URLEncoder converts space to '+', but we need '%20' for path segments
            encoded = encoded.replace("+", "%20");
            
            return encoded;
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always supported, this should never happen
            return segment;
        }
    }
    
    /**
     * Encode a full relative URL path
     * Encodes each path segment separately while preserving '/' separators
     * 
     * @param relativePath Relative path (e.g., "/infinite-world/avatar/tải xuống (3).jpg")
     * @return Encoded path (e.g., "/infinite-world/avatar/t%E1%BA%A3i%20xu%E1%BB%91ng%20%283%29.jpg")
     */
    public static String encodeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return relativePath;
        }
        
        // Split by '/' but preserve the leading '/'
        String[] segments = relativePath.split("/", -1);
        StringBuilder encoded = new StringBuilder();
        
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encoded.append("/");
            }
            
            // Encode each segment, but skip empty segments (from leading/trailing slashes)
            if (!segments[i].isEmpty()) {
                encoded.append(encodePathSegment(segments[i]));
            }
        }
        
        return encoded.toString();
    }
    
    /**
     * Normalize relative path to ensure it has leading slash
     * 
     * @param relativePath Path that may or may not have leading slash
     * @return Normalized path with leading slash
     */
    public static String normalizeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return relativePath;
        }
        
        if (!relativePath.startsWith("/")) {
            return "/" + relativePath;
        }
        
        return relativePath;
    }
    
    /**
     * Encode and normalize relative path for storage in DB
     * Path should be stored in DB already encoded, so it can be used directly
     * 
     * @param objectPath Raw object path (e.g., "infinite-world/avatar/tải xuống (3).jpg")
     * @return Encoded relative path ready for storage (e.g., "/infinite-world/avatar/t%E1%BA%A3i%20xu%E1%BB%91ng%20%283%29.jpg")
     */
    public static String encodeAndNormalizeForStorage(String objectPath) {
        if (objectPath == null || objectPath.isEmpty()) {
            return null;
        }
        
        try {
            // Add leading slash if not present
            String withSlash = objectPath.startsWith("/") ? objectPath : "/" + objectPath;
            
            // Encode path segments
            return encodeRelativePath(withSlash);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Build full URL from base URL and stored relative path
     * Stored relative path should already be encoded
     * 
     * @param baseUrl Base URL with host/IP (e.g., "http://localhost:9000")
     * @param storedRelativePath Stored relative path already encoded (e.g., "/infinite-world/avatar/t%E1%BA%A3i%20xu%E1%BB%91ng%20%283%29.jpg")
     * @return Full URL (e.g., "http://localhost:9000/infinite-world/avatar/t%E1%BA%A3i%20xu%E1%BB%91ng%20%283%29.jpg")
     */
    public static String buildFullUrl(String baseUrl, String storedRelativePath) {
        if (baseUrl == null || baseUrl.isEmpty() || storedRelativePath == null || storedRelativePath.isEmpty()) {
            return null;
        }
        
        try {
            // Remove trailing slash from baseUrl if present
            String cleanBaseUrl = baseUrl;
            if (cleanBaseUrl.endsWith("/")) {
                cleanBaseUrl = cleanBaseUrl.substring(0, cleanBaseUrl.length() - 1);
            }
            
            // Ghép baseUrl + stored relative path (already encoded)
            return cleanBaseUrl + storedRelativePath;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Extract file name from relative path
     * 
     * @param relativePath Relative path (e.g., "/infinite-world/avatar/tải xuống (3).jpg")
     * @return File name (e.g., "tải xuống (3).jpg")
     */
    public static String extractFileName(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }
        
        String[] segments = relativePath.split("/");
        return segments[segments.length - 1];
    }
}
