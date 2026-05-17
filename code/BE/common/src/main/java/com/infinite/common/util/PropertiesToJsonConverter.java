package com.infinite.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Utility class to convert .properties files to JSON format
 * Usage: Run this class to convert properties files to JSON
 */
public class PropertiesToJsonConverter {

    public static void main(String[] args) throws IOException {
        String basePath = "common/src/main/resources/i18n/";
        
        // Convert Vietnamese
        convertPropertiesToJson(
            basePath + "messages_vi.properties",
            basePath + "vi.json"
        );
        
        // Convert English
        convertPropertiesToJson(
            basePath + "messages_en.properties",
            basePath + "en.json"
        );
        
        System.out.println("Conversion completed!");
    }

    public static void convertPropertiesToJson(String propertiesPath, String jsonPath) throws IOException {
        // Load properties file
        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(propertiesPath), 
                StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        // Convert to nested map
        Map<String, Object> nestedMap = new LinkedHashMap<>();
        
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            addToNestedMap(nestedMap, key, value);
        }

        // Write to JSON file
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(jsonPath), nestedMap);
        
        System.out.println("Converted: " + propertiesPath + " -> " + jsonPath);
    }

    /**
     * Add a key-value pair to nested map structure
     * Example: "auth.login.success" -> {"auth": {"login": {"success": "value"}}}
     */
    private static void addToNestedMap(Map<String, Object> map, String key, String value) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = map;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!current.containsKey(part)) {
                current.put(part, new LinkedHashMap<String, Object>());
            }
            Object next = current.get(part);
            if (next instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nextMap = (Map<String, Object>) next;
                current = nextMap;
            } else {
                // If there's a conflict (e.g., "auth" is both a value and a parent key),
                // keep the existing value and skip this entry
                System.err.println("Warning: Key conflict for " + key + " at part " + part);
                return;
            }
        }

        current.put(parts[parts.length - 1], value);
    }

    /**
     * Convert nested map back to flat map (for testing)
     */
    public static Map<String, String> flattenMap(Map<String, Object> nestedMap) {
        Map<String, String> flatMap = new LinkedHashMap<>();
        flattenMapRecursive("", nestedMap, flatMap);
        return flatMap;
    }

    private static void flattenMapRecursive(String prefix, Map<String, Object> map, Map<String, String> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                flattenMapRecursive(key, nestedMap, result);
            } else if (value != null) {
                result.put(key, value.toString());
            }
        }
    }
}
