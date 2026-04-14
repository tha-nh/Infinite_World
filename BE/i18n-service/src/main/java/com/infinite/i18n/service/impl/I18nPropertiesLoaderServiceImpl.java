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
            
            
            ClassPathResource resource = new ClassPathResource(filePathWithLanguage);
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream()))) {
                properties.load(reader);
            }
            
            // Load each property to database
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
                if (response.getCode() == code(StatusCode.SUCCESS)) {
                    count++;
                }
            }
            

            
            return ApiResponse.builder()
                    .code(code(StatusCode.SUCCESS))
                    .message(message("i18n.properties.loaded"))
                    .result(String.format("Loaded %d messages for language: %s", count, language))
                    .build();
        } catch (Exception e) {
            return ApiResponse.builder()
                    .code(code(StatusCode.BAD_REQUEST))
                    .message("Error loading from properties: " + e.getMessage())
                    .build();
        }
    }
}
