//package com.infinite.user.config;
//
//import io.minio.MinioClient;
//import lombok.Getter;
//import lombok.Setter;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Getter
//@Setter
//@Configuration
//@ConfigurationProperties(prefix = "minio")
//public class MinIOConfiguration {
//    private String url;
//    private String accessKey;
//    private String secretKey;
//    private String uploadPath;
//    private String bucketName;
//    private String avatarBucketName;
//
//    @Bean
//    public MinioClient minioClient() {
//        return MinioClient.builder()
//                .endpoint(url)
//                .credentials(accessKey, secretKey)
//                .build();
//    }
//}
