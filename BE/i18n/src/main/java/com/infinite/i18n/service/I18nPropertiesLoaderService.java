package com.infinite.i18n.service;

import com.infinite.i18n.dto.response.ApiResponse;

public interface I18nPropertiesLoaderService {
    ApiResponse<Object> loadPropertiesFile(String language);
    
    ApiResponse<Object> loadPropertiesToDatabase(String language);
}
