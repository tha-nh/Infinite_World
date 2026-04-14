package com.infinite.i18n.service.impl;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.dto.response.ApiResponse;
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

import static com.infinite.common.dto.response.Response.code;
import static com.infinite.common.dto.response.Response.message;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class I18nPropertiesLoaderServiceImpl implements I18nPropertiesLoaderService {

    final I18nService i18nService;

    @Value("${i18n.properties-file:/i18n/messages}")
    private String propertiesFilePath;

    /**
     * Load messages from language-specific properties file
     * Format: key.key.key=message
     * Example: user.profile.name=Name
     * 
     * File naming: messages_{language}.properties
     * - messages_en.properties for English
     * - messages_vi.properties for Vietnamese
     */
    @Override
    public ApiResponse<Object> loadPropertiesFile(String language) {
        int count = 0;
        
        try {
            Properties properties = new Properties();
            
            // Construct file path: /i18n/messages_{language}.properties
            String filePathWithLanguage = String.format("%s_%s.properties", 
                propertiesFilePath, language);
            
            log.info("Loading properties from: {}", filePathWithLanguage);
            
            ClassPathResource resource = new ClassPathResource(filePathWithLanguage);
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {
                properties.load(reader);
            }
            
            // Load each property to database
            for (Object keyObj : properties.keySet()) {
                String key = (String) keyObj;
                String messageValue = properties.getProperty(key);
                
                // Skip comments and empty lines
                if (key.startsWith("#") || messageValue == null || messageValue.trim().isEmpty()) {
                    continue;
                }
                
                I18nMessage entity = I18nMessage.builder()
                        .key(key)
                        .message(messageValue)
                        .language(language)
                        .build();
                
                ApiResponse<Object> response = i18nService.saveMessage(language, entity);
                if (response.getCode() == code(StatusCode.SUCCESS)) {
                    count++;
                }
            }
            
            log.info("Loaded {} messages from properties file for language: {}", count, language);
            
            return ApiResponse.builder()
                    .code(code(StatusCode.SUCCESS))
                    .message(message("i18n.properties.loaded"))
                    .result(String.format("Loaded %d messages for language: %s", count, language))
                    .build();
        } catch (Exception e) {
            log.error("Error loading properties file for language: {}", language, e);
            return ApiResponse.builder()
                    .code(code(StatusCode.BAD_REQUEST))
                    .message("Error loading from properties: " + e.getMessage())
                    .build();
        }
    }
}
