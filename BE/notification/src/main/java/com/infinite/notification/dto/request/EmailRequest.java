package com.infinite.notification.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class EmailRequest {
    private String to;
    private String subject;
    private String content;
    private String template;
    private Map<String, Object> variables;
    private boolean isHtml;
}