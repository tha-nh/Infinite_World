package com.infinite.common.service;

import java.util.Map;

/**
 * Service interface for loading JSON messages to database and Redis
 */
public interface JsonMessageLoaderService {
    
    /**
     * Load messages from JSON file to database only
     * @param language Language code (vi, en, etc.)
     * @return Number of messages loaded
     */
    int loadJsonToDatabase(String language);
    
    /**
     * Load messages from JSON file to both database and Redis
     * @param language Language code (vi, en, etc.)
     * @return Map with "database" and "redis" counts
     */
    Map<String, Integer> loadJsonToDatabaseAndCache(String language);
    
    /**
     * Load messages from properties file to database only (backward compatibility)
     * @param language Language code (vi, en, etc.)
     * @return Number of messages loaded
     */
    int loadPropertiesToDatabase(String language);
    
    /**
     * Load messages from properties file to both database and Redis (backward compatibility)
     * @param language Language code (vi, en, etc.)
     * @return Map with "database" and "redis" counts
     */
    Map<String, Integer> loadPropertiesToDatabaseAndCache(String language);
    
    /**
     * Load messages from JSON to Redis only
     * @param language Language code (vi, en, etc.)
     * @return Number of messages loaded to Redis
     */
    int loadJsonToRedis(String language);
    
    /**
     * Load messages from properties to Redis only
     * @param language Language code (vi, en, etc.)
     * @return Number of messages loaded to Redis
     */
    int loadPropertiesToRedis(String language);
    
    /**
     * Sync messages from JSON to database (update existing, insert new)
     * @param language Language code (vi, en, etc.)
     * @return Map with "inserted", "updated", "unchanged" counts
     */
    Map<String, Integer> syncJsonToDatabase(String language);
}
