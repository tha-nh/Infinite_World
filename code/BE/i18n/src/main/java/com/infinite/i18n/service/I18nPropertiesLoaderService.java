package com.infinite.i18n.service;

import com.infinite.common.dto.response.ApiResponse;

public interface I18nPropertiesLoaderService {
    // Legacy methods - load from properties files
    ApiResponse<Object> loadPropertiesToDatabase(String language);
    ApiResponse<Object> loadPropertiesToDatabaseAndCache(String language);
    
    // New methods - load from JSON files
    ApiResponse<Object> loadJsonToDatabase(String language);
    ApiResponse<Object> loadJsonToDatabaseAndCache(String language);
    ApiResponse<Object> loadJsonToRedis(String language);
    
    // Sync method - update database from JSON
    ApiResponse<Object> syncJsonToDatabase(String language);
}
