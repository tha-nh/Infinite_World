package com.infinite.i18n.service;

import com.infinite.common.dto.response.ApiResponse;

public interface I18nPropertiesLoaderService {
    ApiResponse<Object> loadPropertiesFile(String language);
}
