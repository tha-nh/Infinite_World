package com.infinite.common.service.impl;

import com.infinite.common.service.JsonMessageLoaderService;
import com.infinite.common.service.JsonMessageSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation for loading JSON messages to database and Redis
 * This service bridges between JsonMessageSource and the database/Redis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JsonMessageLoaderServiceImpl implements JsonMessageLoaderService {

    private final JsonMessageSource jsonMessageSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public int loadJsonToDatabase(String language) {
        int count = 0;
        
        try {
            // Ensure table exists
            createTableIfNotExists(language);
            
            // Get all messages from JSON
            Map<String, String> messages = jsonMessageSource.getAllMessages(language);
            
            if (messages.isEmpty()) {
                log.warn("No messages found in JSON for language: {}", language);
                return 0;
            }
            
            // Insert messages to database
            for (Map.Entry<String, String> entry : messages.entrySet()) {
                String key = entry.getKey();
                String message = entry.getValue();
                
                // Split key into hierarchical parts (max 10 levels)
                String[] keyParts = splitKey(key);
                
                // Check if key exists
                String checkSql = String.format(
                    "SELECT COUNT(*) FROM i18n_%s WHERE key = ? AND is_deleted = false",
                    language
                );
                
                Integer exists = jdbcTemplate.queryForObject(checkSql, Integer.class, key);
                
                if (exists != null && exists > 0) {
                    // Update existing
                    String updateSql = String.format(
                        "UPDATE i18n_%s SET message = ?, key1 = ?, key2 = ?, key3 = ?, key4 = ?, " +
                        "key5 = ?, key6 = ?, key7 = ?, key8 = ?, key9 = ?, key10 = ? " +
                        "WHERE key = ? AND is_deleted = false",
                        language
                    );
                    
                    jdbcTemplate.update(updateSql, 
                        message, keyParts[0], keyParts[1], keyParts[2], keyParts[3], keyParts[4],
                        keyParts[5], keyParts[6], keyParts[7], keyParts[8], keyParts[9], key);
                } else {
                    // Insert new
                    String insertSql = String.format(
                        "INSERT INTO i18n_%s (key, message, key1, key2, key3, key4, key5, " +
                        "key6, key7, key8, key9, key10, is_deleted, language) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?)",
                        language
                    );
                    
                    jdbcTemplate.update(insertSql,
                        key, message, keyParts[0], keyParts[1], keyParts[2], keyParts[3], keyParts[4],
                        keyParts[5], keyParts[6], keyParts[7], keyParts[8], keyParts[9], language);
                }
                
                count++;
            }
            
            log.info("Successfully loaded {} messages from JSON to database for language: {}", count, language);
            
        } catch (Exception e) {
            log.error("Error loading JSON to database for language {}: ", language, e);
            throw new RuntimeException("Failed to load JSON to database: " + e.getMessage(), e);
        }
        
        return count;
    }

    @Override
    @Transactional
    public Map<String, Integer> loadJsonToDatabaseAndCache(String language) {
        Map<String, Integer> result = new HashMap<>();
        
        try {
            // Load to database
            int dbCount = loadJsonToDatabase(language);
            result.put("database", dbCount);
            
            // Load to Redis
            int redisCount = jsonMessageSource.loadJsonToRedis(language);
            result.put("redis", redisCount);
            
            log.info("Successfully loaded {} messages to database and {} to Redis for language: {}", 
                dbCount, redisCount, language);
            
        } catch (Exception e) {
            log.error("Error loading JSON to database and cache for language {}: ", language, e);
            throw new RuntimeException("Failed to load JSON to database and cache: " + e.getMessage(), e);
        }
        
        return result;
    }

    @Override
    @Transactional
    public int loadPropertiesToDatabase(String language) {
        int count = 0;
        
        try {
            // Ensure table exists
            createTableIfNotExists(language);
            
            // Get all messages from properties file
            Map<String, String> messages = jsonMessageSource.getMessagePropertiesFromFile(language);
            
            if (messages.isEmpty()) {
                log.warn("No messages found in properties file for language: {}", language);
                return 0;
            }
            
            // Insert messages to database
            for (Map.Entry<String, String> entry : messages.entrySet()) {
                String key = entry.getKey();
                String message = entry.getValue();
                
                // Split key into hierarchical parts
                String[] keyParts = splitKey(key);
                
                // Check if key exists
                String checkSql = String.format(
                    "SELECT COUNT(*) FROM i18n_%s WHERE key = ? AND is_deleted = false",
                    language
                );
                
                Integer exists = jdbcTemplate.queryForObject(checkSql, Integer.class, key);
                
                if (exists != null && exists > 0) {
                    // Update existing
                    String updateSql = String.format(
                        "UPDATE i18n_%s SET message = ?, key1 = ?, key2 = ?, key3 = ?, key4 = ?, " +
                        "key5 = ?, key6 = ?, key7 = ?, key8 = ?, key9 = ?, key10 = ? " +
                        "WHERE key = ? AND is_deleted = false",
                        language
                    );
                    
                    jdbcTemplate.update(updateSql,
                        message, keyParts[0], keyParts[1], keyParts[2], keyParts[3], keyParts[4],
                        keyParts[5], keyParts[6], keyParts[7], keyParts[8], keyParts[9], key);
                } else {
                    // Insert new
                    String insertSql = String.format(
                        "INSERT INTO i18n_%s (key, message, key1, key2, key3, key4, key5, " +
                        "key6, key7, key8, key9, key10, is_deleted, language) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?)",
                        language
                    );
                    
                    jdbcTemplate.update(insertSql,
                        key, message, keyParts[0], keyParts[1], keyParts[2], keyParts[3], keyParts[4],
                        keyParts[5], keyParts[6], keyParts[7], keyParts[8], keyParts[9], language);
                }
                
                count++;
            }
            
            log.info("Successfully loaded {} messages from properties to database for language: {}", count, language);
            
        } catch (Exception e) {
            log.error("Error loading properties to database for language {}: ", language, e);
            throw new RuntimeException("Failed to load properties to database: " + e.getMessage(), e);
        }
        
        return count;
    }

    @Override
    @Transactional
    public Map<String, Integer> loadPropertiesToDatabaseAndCache(String language) {
        Map<String, Integer> result = new HashMap<>();
        
        try {
            // Load to database
            int dbCount = loadPropertiesToDatabase(language);
            result.put("database", dbCount);
            
            // Load to Redis
            int redisCount = jsonMessageSource.loadPropertiesToRedis(language);
            result.put("redis", redisCount);
            
            log.info("Successfully loaded {} messages to database and {} to Redis for language: {}", 
                dbCount, redisCount, language);
            
        } catch (Exception e) {
            log.error("Error loading properties to database and cache for language {}: ", language, e);
            throw new RuntimeException("Failed to load properties to database and cache: " + e.getMessage(), e);
        }
        
        return result;
    }

    @Override
    public int loadJsonToRedis(String language) {
        return jsonMessageSource.loadJsonToRedis(language);
    }

    @Override
    public int loadPropertiesToRedis(String language) {
        return jsonMessageSource.loadPropertiesToRedis(language);
    }

    @Override
    @Transactional
    public Map<String, Integer> syncJsonToDatabase(String language) {
        Map<String, Integer> result = new HashMap<>();
        int inserted = 0;
        int updated = 0;
        int unchanged = 0;
        
        try {
            // Ensure table exists
            createTableIfNotExists(language);
            
            // Get all messages from JSON
            Map<String, String> messages = jsonMessageSource.getAllMessages(language);
            
            if (messages.isEmpty()) {
                log.warn("No messages found in JSON for language: {}", language);
                result.put("inserted", 0);
                result.put("updated", 0);
                result.put("unchanged", 0);
                return result;
            }
            
            // Sync messages
            for (Map.Entry<String, String> entry : messages.entrySet()) {
                String key = entry.getKey();
                String message = entry.getValue();
                
                // Split key into hierarchical parts
                String[] keyParts = splitKey(key);
                
                // Check if key exists and get current message
                String checkSql = String.format(
                    "SELECT message FROM i18n_%s WHERE key = ? AND is_deleted = false",
                    language
                );
                
                try {
                    String currentMessage = jdbcTemplate.queryForObject(checkSql, String.class, key);
                    
                    if (!message.equals(currentMessage)) {
                        // Update if different
                        String updateSql = String.format(
                            "UPDATE i18n_%s SET message = ?, key1 = ?, key2 = ?, key3 = ?, key4 = ?, " +
                            "key5 = ?, key6 = ?, key7 = ?, key8 = ?, key9 = ?, key10 = ? " +
                            "WHERE key = ? AND is_deleted = false",
                            language
                        );
                        
                        jdbcTemplate.update(updateSql,
                            message, keyParts[0], keyParts[1], keyParts[2], keyParts[3], keyParts[4],
                            keyParts[5], keyParts[6], keyParts[7], keyParts[8], keyParts[9], key);
                        updated++;
                    } else {
                        unchanged++;
                    }
                } catch (Exception e) {
                    // Key doesn't exist, insert new
                    String insertSql = String.format(
                        "INSERT INTO i18n_%s (key, message, key1, key2, key3, key4, key5, " +
                        "key6, key7, key8, key9, key10, is_deleted, language) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?)",
                        language
                    );
                    
                    jdbcTemplate.update(insertSql,
                        key, message, keyParts[0], keyParts[1], keyParts[2], keyParts[3], keyParts[4],
                        keyParts[5], keyParts[6], keyParts[7], keyParts[8], keyParts[9], language);
                    inserted++;
                }
            }
            
            result.put("inserted", inserted);
            result.put("updated", updated);
            result.put("unchanged", unchanged);
            
            log.info("Sync completed for language {}: {} inserted, {} updated, {} unchanged", 
                language, inserted, updated, unchanged);
            
        } catch (Exception e) {
            log.error("Error syncing JSON to database for language {}: ", language, e);
            throw new RuntimeException("Failed to sync JSON to database: " + e.getMessage(), e);
        }
        
        return result;
    }

    /**
     * Create i18n table if not exists
     */
    private void createTableIfNotExists(String language) {
        String tableName = "i18n_" + language;
        
        String createTableSql = String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                key VARCHAR(500) NOT NULL,
                message TEXT,
                key1 VARCHAR(100),
                key2 VARCHAR(100),
                key3 VARCHAR(100),
                key4 VARCHAR(100),
                key5 VARCHAR(100),
                key6 VARCHAR(100),
                key7 VARCHAR(100),
                key8 VARCHAR(100),
                key9 VARCHAR(100),
                key10 VARCHAR(100),
                is_deleted BOOLEAN DEFAULT false,
                language VARCHAR(10),
                UNIQUE KEY unique_key (key)
            )
            """, tableName);
        
        try {
            jdbcTemplate.execute(createTableSql);
            log.info("Table {} created or already exists", tableName);
        } catch (Exception e) {
            log.error("Error creating table {}: ", tableName, e);
            throw new RuntimeException("Failed to create table: " + e.getMessage(), e);
        }
    }

    /**
     * Split key into hierarchical parts (max 10 levels)
     * Example: "email.user.locked.subject" -> ["email", "user", "locked", "subject", null, ...]
     */
    private String[] splitKey(String key) {
        String[] result = new String[10];
        String[] parts = key.split("\\.");
        
        for (int i = 0; i < 10; i++) {
            result[i] = i < parts.length ? parts[i] : null;
        }
        
        return result;
    }
}
