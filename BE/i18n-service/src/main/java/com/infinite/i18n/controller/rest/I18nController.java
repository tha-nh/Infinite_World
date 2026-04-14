package com.infinite.i18n.controller.rest;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.i18n.dto.request.I18nMessageRequest;
import com.infinite.i18n.model.I18nMessage;
import com.infinite.i18n.service.I18nService;
import com.infinite.i18n.service.I18nPropertiesLoaderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "v1/api/i18n", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "i18n", description = "Internationalization Messages")
public class I18nController {
    I18nService i18nService;
    I18nPropertiesLoaderService propertiesLoaderService;

    /**
     * Create or update message via API
     * POST /v1/api/i18n/message?language=en
     */
    @PostMapping("/message")
    public ApiResponse<Object> createMessage(
            @RequestParam String language,
            @RequestBody I18nMessageRequest request) {
        I18nMessage message = I18nMessage.builder()
                .key(request.getKey())
                .message(request.getMessage())
                .language(language)
                .build();
        
        return i18nService.saveMessage(language, message);
    }

    /**
     * Delete message (soft delete)
     * DELETE /v1/api/i18n/message?language=en&key=user.profile.name
     */
    @DeleteMapping("/message")
    public ApiResponse<Object> deleteMessage(
            @RequestParam String language,
            @RequestParam String key) {
        return i18nService.deleteMessage(language, key);
    }

    /**
     * Load messages from properties file to database
     * POST /v1/api/i18n/load-from-properties?language=en
     */
    @PostMapping("/load-from-properties")
    public ApiResponse<Object> loadFromProperties(
            @RequestParam String language) {
        return propertiesLoaderService.loadPropertiesFile(language);
    }

    /**
     * Refresh Redis cache from database
     * POST /v1/api/i18n/refresh-cache
     */
    @PostMapping("/refresh-cache")
    public ApiResponse<Object> refreshCache() {
        return i18nService.refreshCache();
    }
}
