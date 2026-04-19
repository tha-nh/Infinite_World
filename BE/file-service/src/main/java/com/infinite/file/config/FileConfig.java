package com.infinite.file.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileConfig {
    
    @Value("${minio.endpoint}")
    private String minioEndpoint;
    
    @Value("${minio.access-key}")
    private String minioAccessKey;
    
    @Value("${minio.secret-key}")
    private String minioSecretKey;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    @Value("${file.upload.max-size:52428800}") // 50MB default
    private long maxFileSize;
    
    @Value("${file.upload.allowed-types:jpg,jpeg,png,gif,mp4,avi,mov,pdf,doc,docx}")
    private String allowedTypes;
    
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }
    
    public String getBucketName() {
        return bucketName;
    }
    
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    public String[] getAllowedTypes() {
        return allowedTypes.split(",");
    }
    
    public String getMinioEndpoint() {
        return minioEndpoint;
    }
}