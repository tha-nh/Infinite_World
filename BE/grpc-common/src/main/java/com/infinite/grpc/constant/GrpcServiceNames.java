package com.infinite.grpc.constant;

/**
 * Common gRPC service naming constants
 * Used for consistent service identification across the ecosystem
 * 
 * IMPORTANT: These names MUST match the names in grpc-client.yml
 */
public final class GrpcServiceNames {
    
    private GrpcServiceNames() {
        // Utility class
    }
    
    // Service name constants (match với grpc-client.yml)
    public static final String FILE_SERVICE = "file-service";
    public static final String USER_SERVICE = "user-service";
    public static final String NOTIFICATION_SERVICE = "notification-service";
    // Note: GATEWAY không có vì không expose gRPC
    
    // Client bean names (optional, for explicit bean naming)
    public static final String FILE_CLIENT_BEAN = "fileServiceClient";
    public static final String USER_CLIENT_BEAN = "userServiceClient";
    public static final String NOTIFICATION_CLIENT_BEAN = "notificationServiceClient";
}
