package com.infinite.i18n.controller.rest;

import com.infinite.i18n.dto.response.ApiResponse;
import com.infinite.i18n.dto.response.I18nPageResponse;
import com.infinite.i18n.dto.request.I18nMessageRequest;
import com.infinite.i18n.model.I18nMessage;
import com.infinite.i18n.service.I18nService;
import com.infinite.i18n.service.I18nPropertiesLoaderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "v1/api/i18n", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class I18nController {
    I18nService i18nService;
    I18nPropertiesLoaderService propertiesLoaderService;

    @PostMapping("/message")
    public ApiResponse<Object> createMessage(
            @RequestParam String language,
            @RequestBody I18nMessageRequest request) {
        I18nMessage message = I18nMessage.builder()
                .key(request.getKey())
                .message(request.getMessage())
                .language(language)
                .build();
        
        return i18nService.saveMessage(language, message);
    }

    @DeleteMapping("/message")
    public ApiResponse<Object> deleteMessage(
            @RequestParam String language,
            @RequestParam String key) {
        return i18nService.deleteMessage(language, key);
    }

    @DeleteMapping("/messages/db")
    public ApiResponse<Object> deleteMessagesFromDb(
            @RequestParam String language,
            @RequestBody List<String> keys) {
        return i18nService.deleteMessagesFromDatabase(language, keys);
    }

    @DeleteMapping("/messages")
    public ApiResponse<Object> deleteMessagesFromBoth(
            @RequestParam String language,
            @RequestBody List<String> keys) {
        return i18nService.deleteMessagesFromBoth(language, keys);
    }

    @DeleteMapping("/messages/multi-language")
    public ApiResponse<Object> deleteMessagesMultiLanguage(
            @RequestBody Map<String, List<String>> languageKeysMap) {
        return i18nService.deleteMessagesMultiLanguage(languageKeysMap);
    }

    @DeleteMapping("/messages/db/multi-language")
    public ApiResponse<Object> deleteMessagesFromDbMultiLanguage(
            @RequestBody Map<String, List<String>> languageKeysMap) {
        return i18nService.deleteMessagesFromDatabaseMultiLanguage(languageKeysMap);
    }

    @DeleteMapping("/messages/redis/multi-language")
    public ApiResponse<Object> deleteMessagesFromRedisMultiLanguage(
            @RequestBody Map<String, List<String>> languageKeysMap) {
        return i18nService.deleteMessagesFromCacheMultiLanguage(languageKeysMap);
    }

    @DeleteMapping("/cache/clear")
    public ApiResponse<Object> clearAllCache() {
        return i18nService.clearAllCache();
    }

    @DeleteMapping("/cache/clear/{language}")
    public ApiResponse<Object> clearCacheByLanguage(@PathVariable String language) {
        return i18nService.clearCacheByLanguage(language);
    }

    @DeleteMapping("/cache/clear/languages")
    public ApiResponse<Object> clearCacheByLanguages(@RequestBody List<String> languages) {
        return i18nService.clearCacheByLanguages(languages);
    }

    @PostMapping("/load-from-properties")
    public ApiResponse<Object> loadFromProperties(
            @RequestParam String language) {
        return propertiesLoaderService.loadPropertiesFile(language);
    }

    @PostMapping("/load-properties-to-db")
    public ApiResponse<Object> loadPropertiesToDatabase(
            @RequestParam String language) {
        return propertiesLoaderService.loadPropertiesToDatabase(language);
    }

    @PostMapping("/load-db-to-cache")
    public ApiResponse<Object> loadDatabaseToCache(
            @RequestParam String language) {
        return i18nService.loadDatabaseToCache(language);
    }

    @PostMapping("/refresh-cache")
    public ApiResponse<Object> refreshCache() {
        return i18nService.refreshCache();
    }

    @PostMapping("/create-table")
    public ApiResponse<Object> createTable(@RequestParam String language) {
        try {
            i18nService.createTableIfNotExists(language);
            return ApiResponse.builder()
                    .code(1000)
                    .message("SUCCESS")
                    .result("Tạo bảng i18n_" + language + " thành công")
                    .build();
        } catch (Exception e) {
            return ApiResponse.builder()
                    .code(1001)
                    .message("BAD_REQUEST")
                    .result("Lỗi khi tạo bảng: " + e.getMessage())
                    .build();
        }
    }

    @GetMapping("/debug/keys")
    public ApiResponse<Object> getRedisKeys(@RequestParam(required = false) String pattern) {
        // This endpoint is for debugging only - remove in production
        return i18nService.getRedisKeys(pattern != null ? pattern : "i18n:*");
    }

    @GetMapping("/messages/tree/db")
    public ApiResponse<I18nPageResponse> getMessagesTreeFromDb(
            @RequestParam String language,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String searchKey,
            @RequestParam(required = false) String searchMessage) {
        return i18nService.getMessagesTreeFromDb(language, page, size, searchKey, searchMessage);
    }

    @GetMapping("/messages/tree/redis")
    public ApiResponse<I18nPageResponse> getMessagesTreeFromRedis(
            @RequestParam String language,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String searchKey,
            @RequestParam(required = false) String searchMessage) {
        return i18nService.getMessagesTreeFromRedis(language, page, size, searchKey, searchMessage);
    }

    @GetMapping("/messages/search")
    public ApiResponse<I18nPageResponse> searchMessages(
            @RequestParam String language,
            @RequestParam(required = false) String searchKey,
            @RequestParam(required = false) String searchMessage,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "db") String source) {
        
        if ("redis".equalsIgnoreCase(source)) {
            return i18nService.getMessagesTreeFromRedis(language, page, size, searchKey, searchMessage);
        } else {
            return i18nService.getMessagesTreeFromDb(language, page, size, searchKey, searchMessage);
        }
    }
}
