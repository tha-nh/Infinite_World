package com.infinite.grpc.interceptor;

import com.infinite.grpc.constant.GrpcMetadataKeys;
import io.grpc.*;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Inbound context propagation interceptor
 * Extracts context (request-id, user-id) from gRPC metadata and sets in MDC for logging
 * 
 * NOTE: MDC is set/cleared within onMessage callback to ensure proper lifecycle
 */
@Component
@Order(3)
public class GrpcContextInboundInterceptor implements ServerInterceptor {
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        
        String requestId = headers.get(GrpcMetadataKeys.REQUEST_ID);
        String userId = headers.get(GrpcMetadataKeys.USER_ID);
        
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(call, headers)) {
            
            @Override
            public void onMessage(ReqT message) {
                // Set MDC before processing
                if (requestId != null) MDC.put("requestId", requestId);
                if (userId != null) MDC.put("userId", userId);
                
                try {
                    super.onMessage(message);
                } finally {
                    // Clear MDC after processing
                    MDC.clear();
                }
            }
        };
    }
}
