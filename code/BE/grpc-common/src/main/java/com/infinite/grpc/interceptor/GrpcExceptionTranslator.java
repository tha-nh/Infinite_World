package com.infinite.grpc.interceptor;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.exception.AppException;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Common exception translator for gRPC services
 * Centralizes exception mapping from business exceptions to gRPC status codes
 * Eliminates need for manual try/catch in each gRPC service implementation
 */
@Slf4j
@Component
@Order(1)
public class GrpcExceptionTranslator implements ServerInterceptor {
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        
        return new ExceptionHandlingServerCallListener<>(next.startCall(call, headers), call);
    }
    
    private static class ExceptionHandlingServerCallListener<ReqT> 
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
        
        private final ServerCall<?, ?> call;
        
        ExceptionHandlingServerCallListener(ServerCall.Listener<ReqT> delegate, ServerCall<?, ?> call) {
            super(delegate);
            this.call = call;
        }
        
        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (AppException e) {
                // Business errors - map to proper gRPC status
                Status grpcStatus = mapToGrpcStatus(e.getStatusCode());
                call.close(grpcStatus.withDescription(e.getMessage()), new Metadata());
            } catch (IllegalArgumentException e) {
                // Handle validation errors
                log.warn("gRPC validation error: {}", e.getMessage());
                call.close(Status.INVALID_ARGUMENT.withDescription(e.getMessage()), new Metadata());
            } catch (Exception e) {
                // Unexpected errors
                log.error("gRPC unexpected error: {}", e.getMessage(), e);
                call.close(Status.INTERNAL.withDescription("Internal server error"), new Metadata());
            }
        }
        
        /**
         * Map StatusCode enum to gRPC Status
         * Uses correct enum names from codebase
         */
        private Status mapToGrpcStatus(StatusCode code) {
            return switch (code) {
                case BAD_REQUEST, PARAM_NULL, INVALID_KEY, FILE_NOT_READ, FILE_NOT_DOWNLOAD, FILE_NOT_DELETED ->
                        Status.INVALID_ARGUMENT;
                case DATA_NOT_EXISTED, FILE_NOT_EXISTED ->
                        Status.NOT_FOUND;
                case UNAUTHORIZED ->
                        Status.UNAUTHENTICATED;
                case NOT_PERMIT ->
                        Status.PERMISSION_DENIED;
                case DUPLICATE, DATA_EXISTED ->
                        Status.ALREADY_EXISTS;
                default ->
                        Status.INTERNAL;
            };
        }
    }
}
