package com.infinite.i18n.service;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.i18n.model.I18nMessage;

public interface I18nService {
    void createTableIfNotExists(String language);

    ApiResponse<Object> saveMessage(String language, I18nMessage msg);

    String getMessage(String key, String language);

    ApiResponse<Object> deleteMessage(String language, String key);

    ApiResponse<Object> refreshCache();
}

