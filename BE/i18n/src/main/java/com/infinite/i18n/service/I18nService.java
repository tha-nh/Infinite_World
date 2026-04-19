package com.infinite.i18n.service;

import com.infinite.i18n.dto.response.ApiResponse;
import com.infinite.i18n.dto.response.I18nPageResponse;
import com.infinite.i18n.model.I18nMessage;

import java.util.List;
import java.util.Map;

public interface I18nService {
    void createTableIfNotExists(String language);

    ApiResponse<Object> saveMessage(String language, I18nMessage msg);

    String getMessage(String key, String language);

    ApiResponse<Object> deleteMessage(String language, String key);

    ApiResponse<Object> deleteMessagesFromDatabase(String language, List<String> keys);

    ApiResponse<Object> deleteMessagesFromCache(String language, List<String> keys);

    ApiResponse<Object> deleteMessagesFromBoth(String language, List<String> keys);

    ApiResponse<Object> deleteMessagesMultiLanguage(Map<String, List<String>> languageKeysMap);

    ApiResponse<Object> deleteMessagesFromDatabaseMultiLanguage(Map<String, List<String>> languageKeysMap);

    ApiResponse<Object> deleteMessagesFromCacheMultiLanguage(Map<String, List<String>> languageKeysMap);

    ApiResponse<Object> clearAllCache();

    ApiResponse<Object> clearCacheByLanguage(String language);

    ApiResponse<Object> clearCacheByLanguages(List<String> languages);

    ApiResponse<Object> refreshCache();
    
    ApiResponse<Object> loadDatabaseToCache(String language);
    
    ApiResponse<Object> getRedisKeys(String pattern);

    ApiResponse<I18nPageResponse> getMessagesTreeFromDb(String language, Integer page, Integer size, 
                                                        String searchKey, String searchMessage);

    ApiResponse<I18nPageResponse> getMessagesTreeFromRedis(String language, Integer page, Integer size, 
                                                           String searchKey, String searchMessage);
}

