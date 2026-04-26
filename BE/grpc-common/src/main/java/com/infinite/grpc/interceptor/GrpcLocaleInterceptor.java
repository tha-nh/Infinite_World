package com.infinite.grpc.interceptor;

import com.infinite.grpc.constant.GrpcMetadataKeys;
import io.grpc.*;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Common i18n interceptor for gRPC services
 * Reads accept-language from metadata and sets locale for request processing
 * 
 * NOTE: This pattern is suitable for unary RPC (simple request-response)
 * For streaming RPC, lifecycle handling needs adjustment
 */
@Component
@Order(2)
public class GrpcLocaleInterceptor implements ServerInterceptor {
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        
        String language = headers.get(GrpcMetadataKeys.ACCEPT_LANGUAGE);
        
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(call, headers)) {
            
            private Locale originalLocale;
            
            @Override
            public void onMessage(ReqT message) {
                // Set locale before processing (works for unary RPC)
                originalLocale = LocaleContextHolder.getLocale();
                if (language != null) {
                    LocaleContextHolder.setLocale(Locale.forLanguageTag(language));
                }
                try {
                    super.onMessage(message);
                } finally {
                    // Clear locale after processing
                    LocaleContextHolder.setLocale(originalLocale);
                }
            }
        };
    }
}
