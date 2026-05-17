package com.infinite.i18n.service.impl;

import com.infinite.common.dto.response.ApiResponse;
import com.infinite.common.service.JsonMessageLoaderService;
import com.infinite.common.service.JsonMessageSource;
import com.infinite.common.util.MessageUtils;
import com.infinite.i18n.model.I18nMessage;
import com.infinite.i18n.service.I18nService;
import com.infinite.i18n.service.I18nPropertiesLoaderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class I18nPropertiesLoaderServiceImpl implements I18nPropertiesLoaderService {

    final I18nService i18nService;
    final JsonMessageSource jsonMessageSource;
    final JsonMessageLoaderService jsonMessageLoaderService;

    @Value("${i18n.properties-file:/i18n/messages}")
    private String propertiesFilePath;

    // ==================== LEGACY METHODS - Properties Files ====================

    @Override
    public ApiResponse<Object> loadPropertiesToDatabase(String language) {
        int count = 0;
        
        try {
            Properties properties = new Properties();
            
            // Try to load from common module's i18n folder first
            String filePathWithLanguage = String.format("%s_%s.properties", 
                propertiesFilePath, language);
            
            log.info("Loading properties from classpath: {}", filePathWithLanguage);
            
            ClassPathResource resource = new ClassPathResource(filePathWithLanguage);
            
            if (!resource.exists()) {
                log.error("Properties file not found in classpath: {}", filePathWithLanguage);
                return ApiResponse.builder()
                        .code(1001)
                        .message(MessageUtils.getMessage("i18n.properties.file.not.found", filePathWithLanguage))
                        .build();
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), "UTF-8"))) {
                properties.load(reader);
                log.info("Loaded {} properties from file for language: {}", properties.size(), language);
            }
            
            for (Object keyObj : properties.keySet()) {
                String key = (String) keyObj;
                String messageValue = properties.getProperty(key);
                
                if (key.startsWith("#") || messageValue == null || messageValue.trim().isEmpty()) {
                    continue;
                }
                
                I18nMessage entity = I18nMessage.builder()
                        .key(key)
                        .message(messageValue)
                        .language(language)
                        .build();
                
                ApiResponse<Object> response = i18nService.saveMessage(language, entity);
                if (response.getCode() == 1000) {
                    count++;
                }
            }
            
            log.info("Successfully loaded {} messages to database for language: {}", count, language);
            
            return ApiResponse.builder()
                    .code(1000)
                    .message(MessageUtils.getMessage("SUCCESS"))
                    .result(String.format(MessageUtils.getMessage("i18n.properties.loaded.count"), count, language))
                    .build();
        } catch (Exception e) {
            log.error("Error loading from properties for language {}: ", language, e);
            return ApiResponse.builder()
                    .code(1001)
                    .message(MessageUtils.getMessage("i18n.properties.load.error") + ": " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> loadPropertiesToDatabaseAndCache(String language) {
        int count = 0;
        
        try {
            Properties properties = new Properties();
            
            String filePathWithLanguage = String.format("%s_%s.properties", 
                propertiesFilePath, language);
            
            log.info("Loading properties from classpath: {}", filePathWithLanguage);
            
            ClassPathResource resource = new ClassPathResource(filePathWithLanguage);
            
            if (!resource.exists()) {
                log.error("Properties file not found in classpath: {}", filePathWithLanguage);
                return ApiResponse.builder()
                        .code(1001)
                        .message(MessageUtils.getMessage("i18n.properties.file.not.found", filePathWithLanguage))
                        .build();
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), "UTF-8"))) {
                properties.load(reader);
                log.info("Loaded {} properties from file for language: {}", properties.size(), language);
            }
            
            // Load to database
            for (Object keyObj : properties.keySet()) {
                String key = (String) keyObj;
                String messageValue = properties.getProperty(key);
                
                if (key.startsWith("#") || messageValue == null || messageValue.trim().isEmpty()) {
                    continue;
                }
                
                I18nMessage entity = I18nMessage.builder()
                        .key(key)
                        .message(messageValue)
                        .language(language)
                        .build();
                
                ApiResponse<Object> response = i18nService.saveMessage(language, entity);
                if (response.getCode() == 1000) {
                    count++;
                }
            }
            
            log.info("Successfully loaded {} messages to database for language: {}", count, language);
            
            // Load to cache
            log.info("Loading from database to Redis cache for language: {}", language);
            ApiResponse<Object> cacheResult = i18nService.loadDatabaseToCache(language);
            
            if (cacheResult.getCode() == 1000) {
                return ApiResponse.builder()
                        .code(1000)
                        .message(MessageUtils.getMessage("SUCCESS"))
                        .result(String.format(MessageUtils.getMessage("i18n.properties.loaded.with.cache.count"), count, language))
                        .build();
            }
            
            return ApiResponse.builder()
                    .code(1000)
                    .message(MessageUtils.getMessage("SUCCESS"))
                    .result(String.format(MessageUtils.getMessage("i18n.properties.loaded.cache.failed"), cacheResult.getMessage()))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error loading from properties for language {}: ", language, e);
            return ApiResponse.builder()
                    .code(1001)
                    .message(MessageUtils.getMessage("i18n.properties.load.error") + ": " + e.getMessage())
                    .build();
        }
    }

    // ==================== NEW METHODS - JSON Files ====================

    @Override
    public ApiResponse<Object> loadJsonToDatabase(String language) {
        try {
            int count = jsonMessageLoaderService.loadJsonToDatabase(language);
            
            return ApiResponse.builder()
                    .code(1000)
                    .message(MessageUtils.getMessage("SUCCESS"))
                    .result(String.format("Loaded %d messages from JSON to database for language: %s", count, language))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error loading JSON to database for language {}: ", language, e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("Error loading JSON to database: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> loadJsonToDatabaseAndCache(String language) {
        try {
            Map<String, Integer> result = jsonMessageLoaderService.loadJsonToDatabaseAndCache(language);
            
            return ApiResponse.builder()
                    .code(1000)
                    .message(MessageUtils.getMessage("SUCCESS"))
                    .result(String.format("Loaded %d messages to database and %d to Redis for language: %s", 
                        result.get("database"), result.get("redis"), language))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error loading JSON to database and cache for language {}: ", language, e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("Error loading JSON to database and cache: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> loadJsonToRedis(String language) {
        try {
            int count = jsonMessageLoaderService.loadJsonToRedis(language);
            
            return ApiResponse.builder()
                    .code(1000)
                    .message(MessageUtils.getMessage("SUCCESS"))
                    .result(String.format("Loaded %d messages from JSON to Redis for language: %s", count, language))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error loading JSON to Redis for language {}: ", language, e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("Error loading JSON to Redis: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> syncJsonToDatabase(String language) {
        try {
            Map<String, Integer> result = jsonMessageLoaderService.syncJsonToDatabase(language);
            
            return ApiResponse.builder()
                    .code(1000)
                    .message(MessageUtils.getMessage("SUCCESS"))
                    .result(String.format("Sync completed for language %s: %d inserted, %d updated, %d unchanged", 
                        language, result.get("inserted"), result.get("updated"), result.get("unchanged")))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error syncing JSON to database for language {}: ", language, e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("Error syncing JSON to database: " + e.getMessage())
                    .build();
        }
    }
}
