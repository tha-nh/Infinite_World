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
    
    @Value("${file.upload.max-size.images:10485760}") // 10MB for images
    private long maxImageSize;
    
    @Value("${file.upload.max-size.avatar:5242880}") // 5MB for avatar
    private long maxAvatarSize;
    
    @Value("${file.upload.max-size.videos:104857600}") // 100MB for videos
    private long maxVideoSize;
    
    @Value("${file.upload.max-size.documents:20971520}") // 20MB for documents
    private long maxDocumentSize;
    
    @Value("${file.upload.max-size.archives:52428800}") // 50MB for archives
    private long maxArchiveSize;
    
    @Value("${file.upload.max-size.default:52428800}") // 50MB default
    private long maxDefaultSize;
    
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
    
    public long getMaxImageSize() {
        return maxImageSize;
    }
    
    public long getMaxAvatarSize() {
        return maxAvatarSize;
    }
    
    public long getMaxVideoSize() {
        return maxVideoSize;
    }
    
    public long getMaxDocumentSize() {
        return maxDocumentSize;
    }
    
    public long getMaxArchiveSize() {
        return maxArchiveSize;
    }
    
    public long getMaxDefaultSize() {
        return maxDefaultSize;
    }
    
    public String[] getAllowedTypes() {
        return allowedTypes.split(",");
    }
    
    public String getMinioEndpoint() {
        return minioEndpoint;
    }
}