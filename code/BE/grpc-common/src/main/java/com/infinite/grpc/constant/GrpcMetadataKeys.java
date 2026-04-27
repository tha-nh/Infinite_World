package com.infinite.grpc.constant;

import io.grpc.Metadata;

/**
 * Common gRPC metadata keys for context propagation
 * Used across all gRPC services for consistent metadata handling
 */
public final class GrpcMetadataKeys {
    
    private GrpcMetadataKeys() {
        // Utility class
    }
    
    public static final Metadata.Key<String> ACCEPT_LANGUAGE = 
        Metadata.Key.of("accept-language", Metadata.ASCII_STRING_MARSHALLER);
    
    public static final Metadata.Key<String> AUTHORIZATION = 
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    
    public static final Metadata.Key<String> REQUEST_ID = 
        Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER);
    
    public static final Metadata.Key<String> USER_ID = 
        Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);
    
    public static final Metadata.Key<String> TENANT_ID = 
        Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);
}
