package com.infinite.grpc.interceptor;

import com.infinite.grpc.constant.GrpcMetadataKeys;
import io.grpc.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Outbound context propagation interceptor
 * Propagates context (request-id, user-id) from MDC to gRPC metadata when calling other services
 * 
 * This ensures context flows through the entire service call chain
 */
@Component
public class GrpcContextOutboundInterceptor implements ClientInterceptor {
    
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // Propagate context từ MDC
                String requestId = MDC.get("requestId");
                String userId = MDC.get("userId");
                
                if (requestId != null) headers.put(GrpcMetadataKeys.REQUEST_ID, requestId);
                if (userId != null) headers.put(GrpcMetadataKeys.USER_ID, userId);
                
                super.start(responseListener, headers);
            }
        };
    }
}
