package com.infinite.notification.contract.builder;

import com.infinite.notification.contract.constant.SchemaVersion;
import com.infinite.notification.contract.dto.NotificationAction;
import com.infinite.notification.contract.dto.NotificationContent;
import com.infinite.notification.contract.dto.NotificationReward;
import com.infinite.notification.contract.dto.NotificationTarget;
import com.infinite.notification.contract.enumtype.NotificationChannel;
import com.infinite.notification.contract.event.NotificationRequestEvent;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Builder helper for creating NotificationRequestEvent
 * Provides fluent API and sensible defaults
 */
public class NotificationRequestBuilder {
    
    private final NotificationRequestEvent.NotificationRequestEventBuilder<?, ?> builder;
    private final Set<NotificationChannel> channels = new HashSet<>();
    
    private NotificationRequestBuilder() {
        this.builder = NotificationRequestEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .schemaVersion(SchemaVersion.V1)
                .occurredAt(Instant.now());
    }
    
    public static NotificationRequestBuilder create() {
        return new NotificationRequestBuilder();
    }
    
    public NotificationRequestBuilder eventId(String eventId) {
        builder.eventId(eventId);
        return this;
    }
    
    public NotificationRequestBuilder requestId(String requestId) {
        builder.requestId(requestId);
        return this;
    }
    
    public NotificationRequestBuilder traceId(String traceId) {
        builder.traceId(traceId);
        return this;
    }
    
    public NotificationRequestBuilder sourceService(String sourceService) {
        builder.sourceService(sourceService);
        return this;
    }
    
    public NotificationRequestBuilder sourceModule(String sourceModule) {
        builder.sourceModule(sourceModule);
        return this;
    }
    
    public NotificationRequestBuilder sourceAction(String sourceAction) {
        builder.sourceAction(sourceAction);
        return this;
    }
    
    public NotificationRequestBuilder requestedBy(String requestedBy) {
        builder.requestedBy(requestedBy);
        return this;
    }
    
    public NotificationRequestBuilder idempotencyKey(String idempotencyKey) {
        builder.idempotencyKey(idempotencyKey);
        return this;
    }
    
    public NotificationRequestBuilder addChannel(NotificationChannel channel) {
        channels.add(channel);
        return this;
    }
    
    public NotificationRequestBuilder channels(Set<NotificationChannel> channels) {
        this.channels.clear();
        this.channels.addAll(channels);
        return this;
    }
    
    public NotificationRequestBuilder target(NotificationTarget target) {
        builder.target(target);
        return this;
    }
    
    public NotificationRequestBuilder content(NotificationContent content) {
        builder.content(content);
        return this;
    }
    
    public NotificationRequestBuilder action(NotificationAction action) {
        builder.action(action);
        return this;
    }
    
    public NotificationRequestBuilder reward(NotificationReward reward) {
        builder.reward(reward);
        return this;
    }
    
    public NotificationRequestBuilder startAt(Instant startAt) {
        builder.startAt(startAt);
        return this;
    }
    
    public NotificationRequestBuilder expireAt(Instant expireAt) {
        builder.expireAt(expireAt);
        return this;
    }
    
    public NotificationRequestBuilder metadata(Map<String, Object> metadata) {
        builder.metadata(metadata);
        return this;
    }
    
    public NotificationRequestEvent build() {
        builder.channels(channels);
        return builder.build();
    }
}
