package com.infinite.i18n.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class I18nMessageRequest {
    @NotBlank(message = "Message key is required")
    private String key;      // e.g., "user.profile.name"
    
    @NotBlank(message = "Message is required")
    private String message;  // The translated message
}
