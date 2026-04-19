package com.infinite.i18n.service;

import com.infinite.common.dto.response.ApiResponse;

public interface I18nPropertiesLoaderService {
    ApiResponse<Object> loadPropertiesToDatabase(String language);
    ApiResponse<Object> loadPropertiesToDatabaseAndCache(String language);
}
