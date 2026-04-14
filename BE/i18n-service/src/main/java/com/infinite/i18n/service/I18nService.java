package com.infinite.i18n.service;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.i18n.model.I18nMessage;

public interface I18nService {
    /**
     * Create table for language if not exists
     */
    void createTableIfNotExists(String language);

    /**
     * Save message to database and cache
     * Auto-creates table if language doesn't exist
     */
    ApiResponse<Object> saveMessage(String language, I18nMessage msg);

    /**
     * Get message from Redis (with DB fallback)
     */
    String getMessage(String key, String language);

    /**
     * Delete message (soft delete)
     */
    ApiResponse<Object> deleteMessage(String language, String key);

    /**
     * Reload all messages from database to Redis
     */
    ApiResponse<Object> refreshCache();
}

