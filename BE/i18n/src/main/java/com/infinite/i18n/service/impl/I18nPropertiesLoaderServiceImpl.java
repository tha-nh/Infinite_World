package com.infinite.i18n.service.impl;

import com.infinite.i18n.dto.response.ApiResponse;
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
    public ApiResponse<Object> loadPropertiesFile(String language) {
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
                        .message("Không tìm thấy file properties: " + filePathWithLanguage)
                        .build();
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {
                properties.load(reader);
                log.info("Loaded {} properties from file", properties.size());
            }
            
            // Load each property to database
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
                    log.debug("Saved message: {}", key);
                } else {
                    log.warn("Failed to save message: {} - {}", key, response.getMessage());
                }
            }
            
            log.info("Successfully loaded {} messages for language: {}", count, language);
            
            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result(String.format("Đã load %d message cho ngôn ngữ: %s", count, language))
                    .build();
        } catch (Exception e) {
            log.error("Error loading from properties: ", e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("LỖI: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> loadPropertiesToDatabase(String language) {
        // Same implementation as loadPropertiesFile but without caching
        return loadPropertiesFile(language);
    }
}
