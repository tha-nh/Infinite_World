package com.infinite.grpc.exception;

import lombok.Getter;

/**
 * Custom exception for gRPC client errors
 * Provides structured error information from gRPC service responses
 */
@Getter
public class GrpcClientException extends RuntimeException {
    
    private final String serviceName;
    private final int businessCode;
    private final String businessMessage;
    
    public GrpcClientException(String serviceName, int businessCode, String businessMessage) {
        super(String.format("[%s] Code: %d, Message: %s", serviceName, businessCode, businessMessage));
        this.serviceName = serviceName;
        this.businessCode = businessCode;
        this.businessMessage = businessMessage;
    }
    
    public GrpcClientException(String serviceName, int businessCode, String businessMessage, Throwable cause) {
        super(String.format("[%s] Code: %d, Message: %s", serviceName, businessCode, businessMessage), cause);
        this.serviceName = serviceName;
        this.businessCode = businessCode;
        this.businessMessage = businessMessage;
    }
}
