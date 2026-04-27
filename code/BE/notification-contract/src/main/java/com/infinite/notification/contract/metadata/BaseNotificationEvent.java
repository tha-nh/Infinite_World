package com.infinite.notification.contract.metadata;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Base metadata for all notification events
 * Provides common fields for tracing, idempotency, and auditing
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseNotificationEvent {
    
    /**
     * Unique event identifier
     */
    private String eventId;
    
    /**
     * Request identifier from caller
     */
    private String requestId;
    
    /**
     * Distributed tracing identifier
     */
    private String traceId;
    
    /**
     * Source service name
     */
    private String sourceService;
    
    /**
     * Source module within service
     */
    private String sourceModule;
    
    /**
     * Source action that triggered this notification
     */
    private String sourceAction;
    
    /**
     * Contract schema version
     */
    private String schemaVersion;
    
    /**
     * When the event occurred
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant occurredAt;
    
    /**
     * User/system that requested this notification
     */
    private String requestedBy;
    
    /**
     * Idempotency key to prevent duplicate processing
     */
    private String idempotencyKey;
}
