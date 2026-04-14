package com.infinite.i18n.service;

import com.infinite.common.dto.response.ApiResponse;

public interface I18nPropertiesLoaderService {
    /**
     * Load messages from language-specific properties file
     * Format: key.key.key=message
     * Example: user.profile.name=Name
     * 
     * File naming: messages_{language}.properties
     * - messages_en.properties for English
     * - messages_vi.properties for Vietnamese
     */
    ApiResponse<Object> loadPropertiesFile(String language);
}
