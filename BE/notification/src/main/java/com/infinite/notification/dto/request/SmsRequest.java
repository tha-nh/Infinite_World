package com.infinite.notification.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SmsRequest {
    private String phoneNumber;
    private String message;
    private String template;
    private Map<String, Object> variables;
}