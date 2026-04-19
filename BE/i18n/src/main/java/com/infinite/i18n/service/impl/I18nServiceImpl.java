package com.infinite.i18n.service.impl;

import com.infinite.i18n.dto.response.ApiResponse;
import com.infinite.i18n.dto.response.I18nPageResponse;
import com.infinite.i18n.dto.response.I18nTreeNode;
import com.infinite.i18n.model.I18nMessage;
import com.infinite.i18n.service.I18nService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class I18nServiceImpl implements I18nService {

    final RedisTemplate<String, String> redisTemplate;
    final JdbcTemplate jdbcTemplate;

    @Value("${i18n.cache-prefix:i18n:}")
    private String cachePrefix;

    @Value("${i18n.languages:en,vi}")
    private String languages;

    @Value("${spring.jpa.properties.hibernate.default_schema:public}")
    private String defaultSchema;

    private static final int MAX_KEY_LEVELS = 10;
    private static final int MIN_KEY_LEVELS = 1;

    @Override
    public void createTableIfNotExists(String language) {
        String tableName = "i18n_" + language;
        String fullTableName = defaultSchema + "." + tableName;

        try {
            String checkTableSql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                    "WHERE table_schema = ? AND table_name = ?)";

            Boolean tableExists = jdbcTemplate.queryForObject(checkTableSql,
                    new Object[]{defaultSchema, tableName}, Boolean.class);

            log.info("Checking table existence for {}: {}", fullTableName, tableExists);

            if (tableExists == null || !tableExists) {
                log.info("Creating table: {}", fullTableName);
                
                StringBuilder createTableSql = new StringBuilder();
                createTableSql.append(String.format("CREATE TABLE %s (\n", fullTableName))
                        .append("  id SERIAL PRIMARY KEY,\n")
                        .append("  key VARCHAR(255) NOT NULL UNIQUE,\n")
                        .append("  message TEXT NOT NULL,\n");

                for (int i = 1; i <= MAX_KEY_LEVELS; i++) {
                    createTableSql.append(String.format("  key_%d VARCHAR(255),\n", i));
                }

                createTableSql.append("  is_deleted BOOLEAN DEFAULT FALSE,\n")
                        .append("  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n")
                        .append("  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n")
                        .append(")");

                log.debug("Executing SQL: {}", createTableSql.toString());
                jdbcTemplate.execute(createTableSql.toString());

                String createIndexSql = String.format(
                        "CREATE INDEX idx_%s_key ON %s(key)",
                        tableName, fullTableName
                );
                jdbcTemplate.execute(createIndexSql);
                
                log.info("Successfully created table: {}", fullTableName);
            } else {
                log.info("Table {} already exists", fullTableName);
            }
        } catch (Exception e) {
            log.error("Error creating table {}: ", fullTableName, e);
            throw new RuntimeException("Failed to create table: " + fullTableName, e);
        }
    }

    @Override
    public ApiResponse<Object> saveMessage(String language, I18nMessage msg) {
        try {
            log.debug("Saving message: {} for language: {}", msg.getKey(), language);
            validateKeyLevels(msg.getKey());
            String[] keyParts = parseMessageKey(msg.getKey());
            String tableName = defaultSchema + ".i18n_" + language;

            StringBuilder sql = new StringBuilder();
            sql.append(String.format("INSERT INTO %s (key, message", tableName));

            for (int i = 0; i < keyParts.length; i++) {
                sql.append(", key_").append(i + 1);
            }

            sql.append(") VALUES (?, ?");

            for (int i = 0; i < keyParts.length; i++) {
                sql.append(", ?");
            }

            sql.append(") ON CONFLICT (key) DO UPDATE SET message = EXCLUDED.message, updated_at = CURRENT_TIMESTAMP");

            for (int i = 0; i < keyParts.length; i++) {
                sql.append(", key_").append(i + 1).append(" = EXCLUDED.key_").append(i + 1);
            }

            Object[] values = new Object[2 + keyParts.length];
            values[0] = msg.getKey();
            values[1] = msg.getMessage();

            for (int i = 0; i < keyParts.length; i++) {
                values[2 + i] = keyParts[i];
            }

            log.debug("Executing SQL: {} with values: {}", sql.toString(), Arrays.toString(values));
            jdbcTemplate.update(sql.toString(), values);

            cacheMessage(language, msg.getKey(), msg.getMessage());
            log.debug("Successfully saved and cached message: {}", msg.getKey());

            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result(msg.getKey())
                    .build();
        } catch (IllegalArgumentException e) {
            log.error("Invalid key format: {}", msg.getKey(), e);
            return ApiResponse.builder()
                    .code(1003)
                    .message("INVALID_KEY")
                    .build();
        } catch (Exception e) {
            log.error("Error saving message: {}", msg.getKey(), e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("BAD_REQUEST: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public String getMessage(String key, String language) {
        String redisKey = cachePrefix + language + ":" + key;

        try {
            String cachedMessage = redisTemplate.opsForValue().get(redisKey);
            if (cachedMessage != null) {
                return cachedMessage;
            }

            String tableName = defaultSchema + ".i18n_" + language;
            String sql = String.format(
                    "SELECT message FROM %s WHERE key = ? AND is_deleted = FALSE",
                    tableName
            );

            String dbMessage = jdbcTemplate.queryForObject(sql, new Object[]{key}, String.class);

            if (dbMessage != null) {
                cacheMessage(language, key, dbMessage);
                return dbMessage;
            }
        } catch (Exception e) {
        }

        return key;
    }

    @Override
    public ApiResponse<Object> deleteMessage(String language, String key) {
        try {
            String tableName = defaultSchema + ".i18n_" + language;
            String sql = String.format(
                    "UPDATE %s SET is_deleted = TRUE WHERE key = ?",
                    tableName
            );

            int updated = jdbcTemplate.update(sql, key);

            if (updated > 0) {
                String redisKey = cachePrefix + language + ":" + key;
                redisTemplate.delete(redisKey);

                return ApiResponse.builder()
                        .code(1000)
                        .message("SUCCESS")
                        .result(key)
                        .build();
            }

            return ApiResponse.builder()
                    .code(1006)
                    .message("DATA_NOT_EXISTED")
                    .build();
        } catch (Exception e) {
            return ApiResponse.builder()
                    .code(1001)
                    .message("BAD_REQUEST")
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> deleteMessagesFromDatabase(String language, List<String> keys) {
        try {
            if (keys == null || keys.isEmpty()) {
                return ApiResponse.builder()
                        .code(1002)
                        .message("PARAM_NULL")
                        .result("Danh sách keys trống")
                        .build();
            }

            String tableName = defaultSchema + ".i18n_" + language;
            String placeholders = String.join(",", keys.stream().map(k -> "?").toArray(String[]::new));
            String sql = String.format(
                    "UPDATE %s SET is_deleted = TRUE WHERE key IN (%s)",
                    tableName, placeholders
            );

            int updated = jdbcTemplate.update(sql, keys.toArray());

            log.info("Deleted {} messages from database for language: {}", updated, language);

            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result(String.format("Đã xóa %d message từ database", updated))
                    .build();
        } catch (Exception e) {
            log.error("Error deleting messages from database: ", e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("BAD_REQUEST: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> deleteMessagesFromCache(String language, List<String> keys) {
        try {
            if (keys == null || keys.isEmpty()) {
                return ApiResponse.builder()
                        .code(1002)
                        .message("PARAM_NULL")
                        .result("Danh sách keys trống")
                        .build();
            }

            int deleted = 0;
            for (String key : keys) {
                String redisKey = cachePrefix + language + ":" + key;
                Boolean result = redisTemplate.delete(redisKey);
                if (Boolean.TRUE.equals(result)) {
                    deleted++;
                }
            }

            log.info("Deleted {} messages from cache for language: {}", deleted, language);

            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result(String.format("Đã xóa %d message từ cache", deleted))
                    .build();
        } catch (Exception e) {
            log.error("Error deleting messages from cache: ", e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("BAD_REQUEST: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> deleteMessagesFromBoth(String language, List<String> keys) {
        try {
            if (keys == null || keys.isEmpty()) {
                return ApiResponse.builder()
                        .code(1002)
                        .message("PARAM_NULL")
                        .result("Danh sách keys trống")
                        .build();
            }

            // Delete from database
            String tableName = defaultSchema + ".i18n_" + language;
            String placeholders = String.join(",", keys.stream().map(k -> "?").toArray(String[]::new));
            String sql = String.format(
                    "UPDATE %s SET is_deleted = TRUE WHERE key IN (%s)",
                    tableName, placeholders
            );

            int dbDeleted = jdbcTemplate.update(sql, keys.toArray());

            // Delete from cache
            int cacheDeleted = 0;
            for (String key : keys) {
                String redisKey = cachePrefix + language + ":" + key;
                Boolean result = redisTemplate.delete(redisKey);
                if (Boolean.TRUE.equals(result)) {
                    cacheDeleted++;
                }
            }

            log.info("Deleted {} messages from database and {} from cache for language: {}", 
                    dbDeleted, cacheDeleted, language);

            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result(String.format("Đã xóa %d message từ database và %d message từ cache cho ngôn ngữ '%s'", 
                            dbDeleted, cacheDeleted, language))
                    .build();
        } catch (Exception e) {
            log.error("Error deleting messages from both: ", e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("BAD_REQUEST: " + e.getMessage())
                    .build();
        }
    }
    @Override
    public ApiResponse<Object> deleteMessagesMultiLanguage(Map<String, List<String>> languageKeysMap) {
        try {
            if (languageKeysMap == null || languageKeysMap.isEmpty()) {
                return ApiResponse.builder()
                        .code(1002)
                        .message("PARAM_NULL")
                        .result("Map ngôn ngữ-keys trống")
                        .build();
            }

            int totalDbDeleted = 0;
            int totalCacheDeleted = 0;
            StringBuilder resultBuilder = new StringBuilder();

            for (Map.Entry<String, List<String>> entry : languageKeysMap.entrySet()) {
                String language = entry.getKey();
                List<String> keys = entry.getValue();

                if (keys == null || keys.isEmpty()) {
                    continue;
                }

                // Delete from database
                String tableName = defaultSchema + ".i18n_" + language;
                String placeholders = String.join(",", keys.stream().map(k -> "?").toArray(String[]::new));
                String sql = String.format(
                        "UPDATE %s SET is_deleted = TRUE WHERE key IN (%s)",
                        tableName, placeholders
                );

                int dbDeleted = jdbcTemplate.update(sql, keys.toArray());
                totalDbDeleted += dbDeleted;

                // Delete from cache
                int cacheDeleted = 0;
                for (String key : keys) {
                    String redisKey = cachePrefix + language + ":" + key;
                    Boolean result = redisTemplate.delete(redisKey);
                    if (Boolean.TRUE.equals(result)) {
                        cacheDeleted++;
                    }
                }
                totalCacheDeleted += cacheDeleted;

                resultBuilder.append(String.format("Ngôn ngữ '%s': %d DB + %d cache; ", 
                        language, dbDeleted, cacheDeleted));

                log.info("Deleted {} messages from database and {} from cache for language: {}", 
                        dbDeleted, cacheDeleted, language);
            }

            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result(String.format("Tổng cộng đã xóa %d message từ database và %d message từ cache. Chi tiết: %s", 
                            totalDbDeleted, totalCacheDeleted, resultBuilder.toString()))
                    .build();
        } catch (Exception e) {
            log.error("Error deleting messages from both multi-language: ", e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("BAD_REQUEST: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> deleteMessagesFromDatabaseMultiLanguage(Map<String, List<String>> languageKeysMap) {
        try {
            if (languageKeysMap == null || languageKeysMap.isEmpty()) {
                return ApiResponse.builder()
                        .code(1002)
                        .message("PARAM_NULL")
                        .result("Map ngôn ngữ-keys trống")
                        .build();
            }

            int totalDeleted = 0;
            StringBuilder resultBuilder = new StringBuilder();

            for (Map.Entry<String, List<String>> entry : languageKeysMap.entrySet()) {
                String language = entry.getKey();
                List<String> keys = entry.getValue();

                if (keys == null || keys.isEmpty()) {
                    continue;
                }

                String tableName = defaultSchema + ".i18n_" + language;
                String placeholders = String.join(",", keys.stream().map(k -> "?").toArray(String[]::new));
                String sql = String.format(
                        "UPDATE %s SET is_deleted = TRUE WHERE key IN (%s)",
                        tableName, placeholders
                );

                int deleted = jdbcTemplate.update(sql, keys.toArray());
                totalDeleted += deleted;

                resultBuilder.append(String.format("Ngôn ngữ '%s': %d messages; ", language, deleted));
                log.info("Deleted {} messages from database for language: {}", deleted, language);
            }

            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result(String.format("Tổng cộng đã xóa %d message từ database. Chi tiết: %s", 
                            totalDeleted, resultBuilder.toString()))
                    .build();
        } catch (Exception e) {
            log.error("Error deleting messages from database multi-language: ", e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("BAD_REQUEST: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> deleteMessagesFromCacheMultiLanguage(Map<String, List<String>> languageKeysMap) {
        try {
            if (languageKeysMap == null || languageKeysMap.isEmpty()) {
                return ApiResponse.builder()
                        .code(1002)
                        .message("PARAM_NULL")
                        .result("Map ngôn ngữ-keys trống")
                        .build();
            }

            int totalDeleted = 0;
            StringBuilder resultBuilder = new StringBuilder();

            for (Map.Entry<String, List<String>> entry : languageKeysMap.entrySet()) {
                String language = entry.getKey();
                List<String> keys = entry.getValue();

                if (keys == null || keys.isEmpty()) {
                    continue;
                }

                int deleted = 0;
                for (String key : keys) {
                    String redisKey = cachePrefix + language + ":" + key;
                    Boolean result = redisTemplate.delete(redisKey);
                    if (Boolean.TRUE.equals(result)) {
                        deleted++;
                    }
                }
                totalDeleted += deleted;

                resultBuilder.append(String.format("Ngôn ngữ '%s': %d messages; ", language, deleted));
                log.info("Deleted {} messages from cache for language: {}", deleted, language);
            }

            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result(String.format("Tổng cộng đã xóa %d message từ cache. Chi tiết: %s", 
                            totalDeleted, resultBuilder.toString()))
                    .build();
        } catch (Exception e) {
            log.error("Error deleting messages from cache multi-language: ", e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("BAD_REQUEST: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> refreshCache() {
        try {
            // Clear all existing cache
            clearAllCache();

            // Reload from all language tables
            List<String> languageList = Arrays.asList(languages.split(","));
            int totalCached = 0;
            
            for (String lang : languageList) {
                String tableName = defaultSchema + ".i18n_" + lang.trim();
                String sql = String.format(
                        "SELECT key, message FROM %s WHERE is_deleted = FALSE",
                        tableName
                );

                try {
                    List<Map<String, Object>> messages = jdbcTemplate.queryForList(sql);
                    for (Map<String, Object> row : messages) {
                        String key = (String) row.get("key");
                        String message = (String) row.get("message");
                        cacheMessage(lang.trim(), key, message);
                        totalCached++;
                    }
                    log.info("Refreshed cache for language: {}, cached {} messages", lang.trim(), messages.size());
                } catch (Exception e) {
                    log.warn("Could not refresh cache for language: {}, table may not exist", lang.trim());
                }
            }

            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result("Đã refresh cache thành công, tổng cộng " + totalCached + " messages")
                    .build();
        } catch (Exception e) {
            log.error("Error refreshing cache: ", e);
            return ApiResponse.builder()
                    .code(9999)
                    .message("INTERNAL_ERROR: " + e.getMessage())
                    .build();
        }
    }
    
    @Override
    public ApiResponse<Object> loadDatabaseToCache(String language) {
        try {
            String tableName = defaultSchema + ".i18n_" + language;
            String sql = String.format(
                    "SELECT key, message FROM %s WHERE is_deleted = FALSE",
                    tableName
            );

            List<Map<String, Object>> messages = jdbcTemplate.queryForList(sql);
            int cached = 0;
            
            for (Map<String, Object> row : messages) {
                String key = (String) row.get("key");
                String message = (String) row.get("message");
                cacheMessage(language, key, message);
                cached++;
            }

            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result(String.format("Đã cache %d message từ database cho ngôn ngữ: %s", cached, language))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error loading database to cache: ", e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("LỖI: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> getRedisKeys(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            Map<String, String> keyValues = new HashMap<>();
            
            if (keys != null) {
                for (String key : keys) {
                    String value = redisTemplate.opsForValue().get(key);
                    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    keyValues.put(key, value + " (TTL: " + ttl + "s)");
                }
            }
            
            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result(keyValues)
                    .build();
        } catch (Exception e) {
            return ApiResponse.builder()
                    .code(9999)
                    .message("INTERNAL_ERROR")
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> clearAllCache() {
        try {
            Set<String> keys = redisTemplate.keys(cachePrefix + "*");
            int deletedCount = 0;
            
            if (keys != null && !keys.isEmpty()) {
                deletedCount = redisTemplate.delete(keys).intValue();
            }

            log.info("Cleared all cache, deleted {} keys", deletedCount);
            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result("Đã xóa toàn bộ cache, tổng cộng " + deletedCount + " keys")
                    .build();
        } catch (Exception e) {
            log.error("Error clearing all cache: ", e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("BAD_REQUEST: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> clearCacheByLanguage(String language) {
        try {
            if (language == null || language.trim().isEmpty()) {
                return ApiResponse.builder()
                        .code(1002)
                        .message("PARAM_NULL")
                        .result("Language không được để trống")
                        .build();
            }

            String pattern = cachePrefix + language.trim() + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            int deletedCount = 0;
            
            if (keys != null && !keys.isEmpty()) {
                deletedCount = redisTemplate.delete(keys).intValue();
            }

            log.info("Cleared cache for language: {}, deleted {} keys", language, deletedCount);
            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result("Đã xóa cache cho ngôn ngữ '" + language + "', tổng cộng " + deletedCount + " keys")
                    .build();
        } catch (Exception e) {
            log.error("Error clearing cache for language: {}", language, e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("BAD_REQUEST: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<Object> clearCacheByLanguages(List<String> languages) {
        try {
            if (languages == null || languages.isEmpty()) {
                return ApiResponse.builder()
                        .code(1002)
                        .message("PARAM_NULL")
                        .result("Danh sách ngôn ngữ trống")
                        .build();
            }

            int totalDeleted = 0;
            StringBuilder resultBuilder = new StringBuilder();

            for (String language : languages) {
                if (language == null || language.trim().isEmpty()) {
                    continue;
                }

                String pattern = cachePrefix + language.trim() + ":*";
                Set<String> keys = redisTemplate.keys(pattern);
                int deletedCount = 0;
                
                if (keys != null && !keys.isEmpty()) {
                    deletedCount = redisTemplate.delete(keys).intValue();
                }
                
                totalDeleted += deletedCount;
                resultBuilder.append(String.format("Ngôn ngữ '%s': %d keys; ", language.trim(), deletedCount));
                log.info("Cleared cache for language: {}, deleted {} keys", language.trim(), deletedCount);
            }

            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result(String.format("Đã xóa cache cho %d ngôn ngữ, tổng cộng %d keys. Chi tiết: %s", 
                            languages.size(), totalDeleted, resultBuilder.toString()))
                    .build();
        } catch (Exception e) {
            log.error("Error clearing cache for multiple languages: ", e);
            return ApiResponse.builder()
                    .code(1001)
                    .message("BAD_REQUEST: " + e.getMessage())
                    .build();
        }
    }
    @Override
    public ApiResponse<I18nPageResponse> getMessagesTreeFromDb(String language, Integer page, Integer size, 
                                                               String searchKey, String searchMessage) {
        try {
            // Validate parameters
            if (page < 1) page = 1;
            if (size < 1 || size > 100) size = 20;
            
            String tableName = defaultSchema + ".i18n_" + language;
            
            // Build search conditions
            StringBuilder whereClause = new StringBuilder("WHERE is_deleted = FALSE");
            List<Object> params = new ArrayList<>();
            
            if (searchKey != null && !searchKey.trim().isEmpty()) {
                whereClause.append(" AND key ILIKE ?");
                params.add("%" + searchKey.trim() + "%");
            }
            
            if (searchMessage != null && !searchMessage.trim().isEmpty()) {
                whereClause.append(" AND message ILIKE ?");
                params.add("%" + searchMessage.trim() + "%");
            }
            
            // Get all matching messages
            String sql = String.format("SELECT key, message FROM %s %s ORDER BY key", 
                                     tableName, whereClause.toString());
            
            List<Map<String, Object>> allMessages = jdbcTemplate.queryForList(sql, params.toArray());
            
            if (allMessages.isEmpty()) {
                return buildEmptyTreeResponse(page, size);
            }
            
            // Convert to common format and build tree
            List<MessageData> messageDataList = allMessages.stream()
                    .map(row -> new MessageData((String) row.get("key"), (String) row.get("message")))
                    .collect(Collectors.toList());
            
            return buildTreeResponse(messageDataList, page, size, searchKey, searchMessage);
            
        } catch (Exception e) {
            log.error("Error getting messages tree from database: ", e);
            return ApiResponse.<I18nPageResponse>builder()
                    .code(1001)
                    .message("LỖI: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ApiResponse<I18nPageResponse> getMessagesTreeFromRedis(String language, Integer page, Integer size, 
                                                                  String searchKey, String searchMessage) {
        try {
            // Validate parameters
            if (page < 1) page = 1;
            if (size < 1 || size > 100) size = 20;
            
            // Get all Redis keys for this language
            String pattern = cachePrefix + language + ":*";
            Set<String> redisKeys = redisTemplate.keys(pattern);
            
            if (redisKeys == null || redisKeys.isEmpty()) {
                return buildEmptyTreeResponse(page, size);
            }
            
            // Convert Redis keys to message data with search filtering
            List<MessageData> messageDataList = new ArrayList<>();
            for (String redisKey : redisKeys) {
                String messageKey = redisKey.replace(cachePrefix + language + ":", "");
                String messageValue = redisTemplate.opsForValue().get(redisKey);
                
                if (messageValue != null) {
                    // Apply search filters
                    boolean matchKey = searchKey == null || searchKey.trim().isEmpty() || 
                                     messageKey.toLowerCase().contains(searchKey.trim().toLowerCase());
                    boolean matchMessage = searchMessage == null || searchMessage.trim().isEmpty() || 
                                         messageValue.toLowerCase().contains(searchMessage.trim().toLowerCase());
                    
                    if (matchKey && matchMessage) {
                        messageDataList.add(new MessageData(messageKey, messageValue));
                    }
                }
            }
            
            if (messageDataList.isEmpty()) {
                return buildEmptyTreeResponse(page, size);
            }
            
            // Sort by key
            messageDataList.sort(Comparator.comparing(MessageData::getKey));
            
            return buildTreeResponse(messageDataList, page, size, searchKey, searchMessage);
            
        } catch (Exception e) {
            log.error("Error getting messages tree from Redis: ", e);
            return ApiResponse.<I18nPageResponse>builder()
                    .code(1001)
                    .message("LỖI: " + e.getMessage())
                    .build();
        }
    }

    // Helper methods
    private void validateKeyLevels(String key) {
        String[] keyParts = parseMessageKey(key);
        if (keyParts.length < MIN_KEY_LEVELS || keyParts.length > MAX_KEY_LEVELS) {
            throw new IllegalArgumentException(
                    String.format("Key must have between %d and %d levels, got %d",
                            MIN_KEY_LEVELS, MAX_KEY_LEVELS, keyParts.length)
            );
        }
    }

    private String[] parseMessageKey(String key) {
        if (key == null || key.isEmpty()) {
            return new String[]{};
        }
        return key.split("\\.");
    }

    private void cacheMessage(String language, String key, String messageValue) {
        try {
            String redisKey = cachePrefix + language + ":" + key;
            // Cache vĩnh viễn - không set TTL
            redisTemplate.opsForValue().set(redisKey, messageValue);
        } catch (Exception e) {
        }
    }
    
    // Common data structure for both DB and Redis messages
    private static class MessageData {
        private final String key;
        private final String message;
        
        public MessageData(String key, String message) {
            this.key = key;
            this.message = message;
        }
        
        public String getKey() { return key; }
        public String getMessage() { return message; }
    }
    
    // Common method to build empty response
    private ApiResponse<I18nPageResponse> buildEmptyTreeResponse(Integer page, Integer size) {
        return ApiResponse.<I18nPageResponse>builder()
                .code(1000)
                .message("SUCCESS")
                .result(I18nPageResponse.builder()
                        .data(new ArrayList<>())
                        .currentPage(page)
                        .pageSize(size)
                        .totalPages(0L)
                        .totalKeys(0L)
                        .totalKey1(0L)
                        .totalRecords(0L)
                        .build())
                .build();
    }
    
    // Common method to build tree response from message data
    private ApiResponse<I18nPageResponse> buildTreeResponse(List<MessageData> messageDataList, 
                                                           Integer page, Integer size,
                                                           String searchKey, String searchMessage) {
        // Group by key1 (first part of key)
        Map<String, List<MessageData>> groupedByKey1 = messageDataList.stream()
                .collect(Collectors.groupingBy(msg -> {
                    String[] parts = msg.getKey().split("\\.");
                    return parts.length > 0 ? parts[0] : msg.getKey();
                }));
        
        // Calculate statistics
        Long totalKeys = (long) messageDataList.size();
        Long totalKey1 = (long) groupedByKey1.size();
        Long totalPages = (totalKey1 + size - 1) / size;
        
        // Apply pagination to key1 level
        List<String> key1List = new ArrayList<>(groupedByKey1.keySet());
        key1List.sort(String::compareTo);
        
        Integer offset = (page - 1) * size;
        List<String> pagedKey1 = key1List.stream()
                .skip(offset)
                .limit(size)
                .collect(Collectors.toList());
        
        // Build tree structure
        List<I18nTreeNode> treeNodes = new ArrayList<>();
        for (String key1 : pagedKey1) {
            List<MessageData> messagesForKey1 = groupedByKey1.get(key1);
            I18nTreeNode rootNode = buildTreeFromMessages(key1, messagesForKey1);
            treeNodes.add(rootNode);
        }
        
        // Build response
        I18nPageResponse.I18nPageResponseBuilder responseBuilder = I18nPageResponse.builder()
                .data(treeNodes)
                .currentPage(page)
                .pageSize(size)
                .totalPages(totalPages)
                .totalKeys(totalKeys)
                .totalKey1(totalKey1)
                .totalRecords((long) treeNodes.size());
        
        // Only set search fields if they have values
        boolean hasSearch = false;
        if (searchKey != null && !searchKey.trim().isEmpty()) {
            responseBuilder.searchKey(searchKey);
            hasSearch = true;
        }
        if (searchMessage != null && !searchMessage.trim().isEmpty()) {
            responseBuilder.searchMessage(searchMessage);
            hasSearch = true;
        }
        if (hasSearch) {
            responseBuilder.hasSearch(true);
        }
        
        I18nPageResponse response = responseBuilder.build();
        
        return ApiResponse.<I18nPageResponse>builder()
                .code(1000)
                .message("SUCCESS")
                .result(response)
                .build();
    }
    
    // Common method to build tree from message data
    private I18nTreeNode buildTreeFromMessages(String key1, List<MessageData> messages) {
        // Find root message (where key equals key1)
        MessageData rootMessage = messages.stream()
                .filter(m -> key1.equals(m.getKey()))
                .findFirst()
                .orElse(null);
        
        I18nTreeNode.I18nTreeNodeBuilder builder = I18nTreeNode.builder()
                .key(key1)
                .fullKey(key1)
                .level(1);
        
        // Only set message if it exists
        if (rootMessage != null) {
            builder.message(rootMessage.getMessage());
        }
        
        I18nTreeNode rootNode = builder.build();
        rootNode.setChildren(new ArrayList<>());
        
        // Build children hierarchy
        buildChildrenRecursive(rootNode, messages, 2);
        
        // Always set totalChildren (even if 0)
        long totalChildren = countTotalChildren(rootNode);
        rootNode.setTotalChildren(totalChildren);
        
        // Remove empty children list if no children
        if (rootNode.getChildren().isEmpty()) {
            rootNode.setChildren(null);
        }
        
        return rootNode;
    }
    
    private void buildChildrenRecursive(I18nTreeNode parent, List<MessageData> messages, int level) {
        if (level > 10) return; // Max 10 levels
        
        // Get distinct values for current level under this parent
        Set<String> childKeys = messages.stream()
                .filter(m -> {
                    String[] parts = m.getKey().split("\\.");
                    if (parts.length < level) return false;
                    
                    // Check if this message belongs to current parent path
                    for (int i = 0; i < level - 1; i++) {
                        String expectedValue = getKeyAtLevel(parent.getFullKey(), i + 1);
                        if (!Objects.equals(expectedValue, parts[i])) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(m -> {
                    String[] parts = m.getKey().split("\\.");
                    return parts.length >= level ? parts[level - 1] : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        for (String childKey : childKeys) {
            String fullKey = parent.getFullKey() + "." + childKey;
            
            // Find message for this full key
            MessageData childMessage = messages.stream()
                    .filter(m -> fullKey.equals(m.getKey()))
                    .findFirst()
                    .orElse(null);
            
            I18nTreeNode.I18nTreeNodeBuilder childBuilder = I18nTreeNode.builder()
                    .key(childKey)
                    .fullKey(fullKey)
                    .level(level);
            
            // Only set message if it exists
            if (childMessage != null) {
                childBuilder.message(childMessage.getMessage());
            }
            
            I18nTreeNode childNode = childBuilder.build();
            childNode.setChildren(new ArrayList<>());
            
            parent.getChildren().add(childNode);
            
            // Recursively build children
            buildChildrenRecursive(childNode, messages, level + 1);
            
            // Always set totalChildren (even if 0)
            long totalChildren = countTotalChildren(childNode);
            childNode.setTotalChildren(totalChildren);
            
            // Remove empty children list if no children
            if (childNode.getChildren().isEmpty()) {
                childNode.setChildren(null);
            }
        }
    }
    
    private String getKeyAtLevel(String fullKey, int level) {
        String[] parts = fullKey.split("\\.");
        return level <= parts.length ? parts[level - 1] : null;
    }
    
    private Long countTotalChildren(I18nTreeNode node) {
        long count = node.getChildren() != null ? node.getChildren().size() : 0;
        if (node.getChildren() != null) {
            for (I18nTreeNode child : node.getChildren()) {
                count += countTotalChildren(child);
            }
        }
        return count;
    }
}