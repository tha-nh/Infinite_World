package com.infinite.grpc.autoconfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.PropertySource;

/**
 * Auto-configuration for gRPC clients
 * Automatically loads grpc-client.yml when grpc-common is imported
 * 
 * Services can override configuration via:
 * 1. Environment variables (e.g., GRPC_FILE_SERVICE_ADDRESS)
 * 2. application.properties/yml in the service
 * 3. Command line arguments
 * 
 * Usage in services:
 * <pre>
 * {@code
 * @GrpcClient("file-service")
 * private FileServiceRpcGrpc.FileServiceRpcBlockingStub fileServiceStub;
 * }
 * </pre>
 */
@Slf4j
@AutoConfiguration
@PropertySource(
        value = "classpath:grpc-client.yml",
        factory = GrpcYamlPropertySourceFactory.class)
public class GrpcClientAutoConfiguration {
    
    public GrpcClientAutoConfiguration() {
        log.info("gRPC Client Auto-Configuration loaded from grpc-common");
    }
}
