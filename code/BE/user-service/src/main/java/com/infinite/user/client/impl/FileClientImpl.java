package com.infinite.user.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinite.common.dto.response.ApiResponse;
import com.infinite.user.client.FileClient;
import com.infinite.common.dto.response.FileUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FileClientImpl implements FileClient {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String fileServiceUrl;
    
    // Cache for file URLs (expire after 6 hours)
    private final ConcurrentHashMap<String, CachedUrl> urlCache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRE_TIME = TimeUnit.HOURS.toMillis(6);
    
    public FileClientImpl(RestTemplate restTemplate, 
                         ObjectMapper objectMapper,
                         String fileServiceUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.fileServiceUrl = fileServiceUrl;
    }
    
    @Override
    @Async
    public void uploadFileAsync(MultipartFile file, String category, String userId, FileUploadCallback callback) {
        try {
            ApiResponse<FileUploadResponse> response = uploadFile(file, category, userId);
            if (response.getCode() == 200) {
                callback.onSuccess(response.getResult());
            } else {
                callback.onError(response.getMessage());
            }
        } catch (Exception e) {
            log.error("Async file upload failed", e);
            callback.onError(e.getMessage());
        }
    }
    
    @Override
    public ApiResponse<FileUploadResponse> uploadFile(MultipartFile file, String category, String userId) {
        try {
            String url = fileServiceUrl + "/v1/api/files/upload";
            
            // Prepare multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("category", category);
            if (userId != null) {
                body.add("userId", userId);
            }
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Make HTTP call with timeout
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, String.class);
            
            // Parse response
            ApiResponse<FileUploadResponse> apiResponse = objectMapper.readValue(
                response.getBody(), 
                objectMapper.getTypeFactory().constructParametricType(
                    ApiResponse.class, FileUploadResponse.class)
            );
            
            // Cache the URL if successful
            if (apiResponse.getCode() == 200 && apiResponse.getResult() != null) {
                String cacheKey = category + "/" + apiResponse.getResult().getFileName();
                urlCache.put(cacheKey, new CachedUrl(apiResponse.getResult().getFileUrl(), System.currentTimeMillis()));
            }
            
            return apiResponse;
            
        } catch (Exception e) {
            log.error("File upload failed", e);
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }
    
    @Override
    public String getFileUrl(String fileName, String category) {
        String cacheKey = category + "/" + fileName;
        
        // Check cache first
        CachedUrl cachedUrl = urlCache.get(cacheKey);
        if (cachedUrl != null && !cachedUrl.isExpired()) {
            return cachedUrl.url;
        }
        
        try {
            String url = fileServiceUrl + "/api/files/" + category + "/" + fileName;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                String fileUrl = response.getBody();
                // Cache the result
                urlCache.put(cacheKey, new CachedUrl(fileUrl, System.currentTimeMillis()));
                return fileUrl;
            }
            
        } catch (Exception e) {
            log.error("Failed to get file URL for: {}/{}", category, fileName, e);
        }
        
        return null;
    }
    
    @Override
    @Async
    public void deleteFileAsync(String fileName, String category) {
        try {
            String url = fileServiceUrl + "/api/files/" + category + "/" + fileName;
            restTemplate.delete(url);
            
            // Remove from cache
            String cacheKey = category + "/" + fileName;
            urlCache.remove(cacheKey);
            
            log.info("File deleted asynchronously: {}/{}", category, fileName);
        } catch (Exception e) {
            log.error("Async file deletion failed for: {}/{}", category, fileName, e);
        }
    }
    
    // Cache entry class
    private static class CachedUrl {
        final String url;
        final long timestamp;
        
        CachedUrl(String url, long timestamp) {
            this.url = url;
            this.timestamp = timestamp;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRE_TIME;
        }
    }
}