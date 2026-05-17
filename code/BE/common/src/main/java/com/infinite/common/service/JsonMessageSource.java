package com.infinite.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to load and retrieve i18n messages from JSON files
 * Also supports loading from properties files and saving to database/Redis
 */
@Slf4j
@Component
public class JsonMessageSource {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Map<String, String>> messages = new ConcurrentHashMap<>();
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${i18n.cache-prefix:i18n:}")
    private String cachePrefix;

    @Value("${i18n.properties-file:/i18n/messages}")
    private String propertiesFilePath;

    public JsonMessageSource(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        loadMessages();
    }

    /**
     * Load all JSON message files from classpath:i18n/*.json
     */
    private void loadMessages() {
        try {
            Resource[] resources = resolver.getResources("classpath:i18n/*.json");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null && filename.endsWith(".json")) {
                    String language = filename.replace(".json", "");
                    loadMessageFile(resource, language);
                }
            }
            log.info("Loaded i18n messages for {} languages", messages.size());
        } catch (IOException e) {
            log.error("Failed to load i18n message files", e);
        }
    }

    /**
     * Load a single JSON message file
     */
    @SuppressWarnings("unchecked")
    private void loadMessageFile(Resource resource, String language) {
        try {
            Map<String, Object> jsonMap = objectMapper.readValue(resource.getInputStream(), Map.class);
            Map<String, String> flatMap = new HashMap<>();
            flattenMap("", jsonMap, flatMap);
            messages.put(language, flatMap);
            log.info("Loaded {} messages for language: {}", flatMap.size(), language);
        } catch (IOException e) {
            log.error("Failed to load message file for language: {}", language, e);
        }
    }

    /**
     * Flatten nested JSON structure to dot-notation keys
     * Example: {"email": {"subject": "Test"}} -> {"email.subject": "Test"}
     */
    private void flattenMap(String prefix, Map<String, Object> map, Map<String, String> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                flattenMap(key, nestedMap, result);
            } else if (value != null) {
                result.put(key, value.toString());
            }
        }
    }

    /**
     * Get message by key and locale
     */
    public String getMessage(String key, Locale locale) {
        String language = locale.getLanguage();
        Map<String, String> languageMessages = messages.get(language);

        if (languageMessages != null && languageMessages.containsKey(key)) {
            return languageMessages.get(key);
        }

        // Fallback to default language (vi)
        Map<String, String> defaultMessages = messages.get("vi");
        if (defaultMessages != null && defaultMessages.containsKey(key)) {
            return defaultMessages.get(key);
        }

        // Return key if not found
        return key;
    }

    /**
     * Get message by key and language code
     */
    public String getMessage(String key, String language) {
        Map<String, String> languageMessages = messages.get(language);

        if (languageMessages != null && languageMessages.containsKey(key)) {
            return languageMessages.get(key);
        }

        // Fallback to default language (vi)
        Map<String, String> defaultMessages = messages.get("vi");
        if (defaultMessages != null && defaultMessages.containsKey(key)) {
            return defaultMessages.get(key);
        }

        // Return key if not found
        return key;
    }

    /**
     * Get all messages for a specific language
     */
    public Map<String, String> getAllMessages(String language) {
        return messages.getOrDefault(language, new HashMap<>());
    }

    /**
     * Get all loaded languages
     */
    public Set<String> getLoadedLanguages() {
        return messages.keySet();
    }

    /**
     * Reload all message files (useful for hot-reload in development)
     */
    public void reload() {
        messages.clear();
        loadMessages();
    }

    // ==================== PROPERTIES FILE SUPPORT ====================

    /**
     * Load messages from old properties file format
     * This is for backward compatibility
     */
    public Map<String, String> getMessagePropertiesFromFile(String language) {
        Map<String, String> result = new HashMap<>();
        
        try {
            Properties properties = new Properties();
            String filePathWithLanguage = String.format("%s_%s.properties", propertiesFilePath, language);
            
            log.info("Loading properties from classpath: {}", filePathWithLanguage);
            
            ClassPathResource resource = new ClassPathResource(filePathWithLanguage);
            
            if (!resource.exists()) {
                log.warn("Properties file not found in classpath: {}", filePathWithLanguage);
                return result;
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                properties.load(reader);
                log.info("Loaded {} properties from file for language: {}", properties.size(), language);
            }
            
            for (Object keyObj : properties.keySet()) {
                String key = (String) keyObj;
                String messageValue = properties.getProperty(key);
                
                if (!key.startsWith("#") && messageValue != null && !messageValue.trim().isEmpty()) {
                    result.put(key, messageValue);
                }
            }
            
        } catch (Exception e) {
            log.error("Error loading properties file for language {}: ", language, e);
        }
        
        return result;
    }

    // ==================== REDIS OPERATIONS ====================

    /**
     * Load messages from JSON to Redis cache
     */
    public int loadJsonToRedis(String language) {
        int count = 0;
        
        try {
            Map<String, String> languageMessages = messages.get(language);
            
            if (languageMessages == null || languageMessages.isEmpty()) {
                log.warn("No messages found for language: {}", language);
                return 0;
            }
            
            for (Map.Entry<String, String> entry : languageMessages.entrySet()) {
                String redisKey = cachePrefix + language + ":" + entry.getKey();
                redisTemplate.opsForValue().set(redisKey, entry.getValue());
                count++;
            }
            
            log.info("Loaded {} messages to Redis for language: {}", count, language);
            
        } catch (Exception e) {
            log.error("Error loading messages to Redis for language {}: ", language, e);
        }
        
        return count;
    }

    /**
     * Load messages from properties file to Redis cache
     */
    public int loadPropertiesToRedis(String language) {
        int count = 0;
        
        try {
            Map<String, String> properties = getMessagePropertiesFromFile(language);
            
            if (properties.isEmpty()) {
                log.warn("No properties found for language: {}", language);
                return 0;
            }
            
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String redisKey = cachePrefix + language + ":" + entry.getKey();
                redisTemplate.opsForValue().set(redisKey, entry.getValue());
                count++;
            }
            
            log.info("Loaded {} properties to Redis for language: {}", count, language);
            
        } catch (Exception e) {
            log.error("Error loading properties to Redis for language {}: ", language, e);
        }
        
        return count;
    }

    /**
     * Clear Redis cache for a specific language
     */
    public int clearRedisCache(String language) {
        int count = 0;
        
        try {
            String pattern = cachePrefix + language + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                count = keys.size();
                redisTemplate.delete(keys);
                log.info("Cleared {} keys from Redis for language: {}", count, language);
            }
            
        } catch (Exception e) {
            log.error("Error clearing Redis cache for language {}: ", language, e);
        }
        
        return count;
    }

    /**
     * Clear all Redis cache for i18n
     */
    public int clearAllRedisCache() {
        int count = 0;
        
        try {
            String pattern = cachePrefix + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                count = keys.size();
                redisTemplate.delete(keys);
                log.info("Cleared all {} i18n keys from Redis", count);
            }
            
        } catch (Exception e) {
            log.error("Error clearing all Redis cache: ", e);
        }
        
        return count;
    }

    /**
     * Get message from Redis
     */
    public String getMessageFromRedis(String key, String language) {
        try {
            String redisKey = cachePrefix + language + ":" + key;
            return redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.error("Error getting message from Redis: ", e);
            return null;
        }
    }

    /**
     * Get all messages from Redis for a language
     */
    public Map<String, String> getAllMessagesFromRedis(String language) {
        Map<String, String> result = new HashMap<>();
        
        try {
            String pattern = cachePrefix + language + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null) {
                String prefix = cachePrefix + language + ":";
                for (String redisKey : keys) {
                    String messageKey = redisKey.substring(prefix.length());
                    String value = redisTemplate.opsForValue().get(redisKey);
                    if (value != null) {
                        result.put(messageKey, value);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error getting all messages from Redis for language {}: ", language, e);
        }
        
        return result;
    }
}
