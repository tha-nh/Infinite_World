package com.infinite.user.config;

import com.infinite.grpc.client.file.FileGrpcClient;
import com.infinite.user.client.FileClient;
import com.infinite.user.client.impl.GrpcFileClientImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * File Client Configuration
 * Uses gRPC implementation to communicate with file-service
 */
@Configuration
@RequiredArgsConstructor
public class FileClientConfig {
    
    private final FileGrpcClient fileGrpcClient;
    
    @Bean
    public FileClient fileClient() {
        return new GrpcFileClientImpl(fileGrpcClient);
    }
}
