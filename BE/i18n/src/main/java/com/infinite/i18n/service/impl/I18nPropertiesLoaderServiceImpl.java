package com.infinite.i18n.service.impl;

import com.infinite.common.dto.response.ApiResponse;
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
import java.util.Properties;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class I18nPropertiesLoaderServiceImpl implements I18nPropertiesLoaderService {

    final I18nService i18nService;

    @Value("${i18n.properties-file:/i18n/messages}")
    private String propertiesFilePath;

    @Override
    public ApiResponse<Object> loadPropertiesToDatabase(String language) {
        int count = 0;
        
        try {
            Properties properties = new Properties();
            
            String filePathWithLanguage = String.format("%s_%s.properties", 
                propertiesFilePath, language);
            
            log.info("Loading properties from: {}", filePathWithLanguage);
            
            ClassPathResource resource = new ClassPathResource(filePathWithLanguage);
            
            if (!resource.exists()) {
                log.error("Properties file not found: {}", filePathWithLanguage);
                return ApiResponse.builder()
                        .code(1001)
                        .message(MessageUtils.getMessage("i18n.properties.file.not.found", filePathWithLanguage))
                        .build();
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {
                properties.load(reader);
                log.info("Loaded {} properties from file", properties.size());
            }
            
            for (Object keyObj : properties.keySet()) {
                String key = (String) keyObj;
                String messageValue = properties.getProperty(key);
                
                log.debug("Processing key: {} = {}", key, messageValue);
                
                if (key.startsWith("#") || messageValue == null || messageValue.trim().isEmpty()) {
                    log.debug("Skipping key: {}", key);
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
                    log.debug("Saved message to DB: {}", key);
                } else {
                    log.warn("Failed to save message: {} - {}", key, response.getMessage());
                }
            }
            
            log.info("Successfully loaded {} messages to database for language: {}", count, language);
            
            return ApiResponse.builder()
                    .code(1000)
                    .message(MessageUtils.getMessage("SUCCESS"))
                    .result(MessageUtils.getMessage("i18n.properties.loaded.count", String.valueOf(count), language))
                    .build();
        } catch (Exception e) {
            log.error("Error loading from properties: ", e);
            return ApiResponse.builder()
                    .code(1001)
                    .message(MessageUtils.getMessage("i18n.properties.load.error") + ": " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> loadPropertiesToDatabaseAndCache(String language) {
        ApiResponse<Object> dbResult = loadPropertiesToDatabase(language);
        
        if (dbResult.getCode() != 1000) {
            return dbResult;
        }
        
        log.info("Loading from database to Redis cache for language: {}", language);
        ApiResponse<Object> cacheResult = i18nService.loadDatabaseToCache(language);
        
        if (cacheResult.getCode() == 1000) {
            return ApiResponse.builder()
                    .code(1000)
                    .message(MessageUtils.getMessage("SUCCESS"))
                    .result(MessageUtils.getMessage("i18n.properties.loaded.with.cache", language))
                    .build();
        }
        
        return ApiResponse.builder()
                .code(1000)
                .message(MessageUtils.getMessage("SUCCESS"))
                .result(MessageUtils.getMessage("i18n.properties.loaded.cache.failed", cacheResult.getMessage()))
                .build();
    }
}
