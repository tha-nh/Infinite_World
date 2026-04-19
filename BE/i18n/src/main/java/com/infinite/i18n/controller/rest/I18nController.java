package com.infinite.i18n.controller.rest;

import com.infinite.i18n.dto.response.ApiResponse;
import com.infinite.i18n.dto.response.I18nPageResponse;
import com.infinite.i18n.dto.request.I18nMessageRequest;
import com.infinite.i18n.model.I18nMessage;
import com.infinite.i18n.service.I18nService;
import com.infinite.i18n.service.I18nPropertiesLoaderService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "I18N Management", description = "APIs quản lý đa ngôn ngữ (Internationalization)")
public class I18nController {
    I18nService i18nService;
    I18nPropertiesLoaderService propertiesLoaderService;

    @Operation(summary = "Tạo hoặc cập nhật message", description = "Tạo mới hoặc cập nhật message đa ngôn ngữ")
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

    @Operation(summary = "Xóa message đơn lẻ", description = "Xóa một message theo key và language")
    @DeleteMapping("/message")
    public ApiResponse<Object> deleteMessage(
            @RequestParam String language,
            @RequestParam String key) {
        return i18nService.deleteMessage(language, key);
    }

    @Operation(summary = "Xóa nhiều messages từ database", description = "Xóa nhiều messages theo danh sách keys từ database")
    @DeleteMapping("/messages/db")
    public ApiResponse<Object> deleteMessagesFromDb(
            @RequestParam String language,
            @RequestBody List<String> keys) {
        return i18nService.deleteMessagesFromDatabase(language, keys);
    }

    @Operation(summary = "Xóa nhiều messages từ cả DB và Redis", description = "Xóa nhiều messages từ cả database và cache")
    @DeleteMapping("/messages")
    public ApiResponse<Object> deleteMessagesFromBoth(
            @RequestParam String language,
            @RequestBody List<String> keys) {
        return i18nService.deleteMessagesFromBoth(language, keys);
    }

    @Operation(summary = "Xóa nhiều messages, nhiều ngôn ngữ", description = "Xóa messages từ cả DB và Redis cho nhiều ngôn ngữ")
    @DeleteMapping("/messages/multi-language")
    public ApiResponse<Object> deleteMessagesMultiLanguage(
            @RequestBody Map<String, List<String>> languageKeysMap) {
        return i18nService.deleteMessagesMultiLanguage(languageKeysMap);
    }

    @Operation(summary = "Xóa nhiều messages từ DB (multi-language)", description = "Xóa messages từ database cho nhiều ngôn ngữ")
    @DeleteMapping("/messages/db/multi-language")
    public ApiResponse<Object> deleteMessagesFromDbMultiLanguage(
            @RequestBody Map<String, List<String>> languageKeysMap) {
        return i18nService.deleteMessagesFromDatabaseMultiLanguage(languageKeysMap);
    }

    @Operation(summary = "Xóa nhiều messages từ Redis (multi-language)", description = "Xóa messages từ cache cho nhiều ngôn ngữ")
    @DeleteMapping("/messages/redis/multi-language")
    public ApiResponse<Object> deleteMessagesFromRedisMultiLanguage(
            @RequestBody Map<String, List<String>> languageKeysMap) {
        return i18nService.deleteMessagesFromCacheMultiLanguage(languageKeysMap);
    }

    @Operation(summary = "Xóa toàn bộ cache i18n", description = "Xóa tất cả cache i18n trong Redis")
    @DeleteMapping("/cache/clear")
    public ApiResponse<Object> clearAllCache() {
        return i18nService.clearAllCache();
    }

    @Operation(summary = "Xóa cache theo ngôn ngữ", description = "Xóa tất cả cache của một ngôn ngữ cụ thể")
    @DeleteMapping("/cache/clear/{language}")
    public ApiResponse<Object> clearCacheByLanguage(@PathVariable String language) {
        return i18nService.clearCacheByLanguage(language);
    }

    @Operation(summary = "Xóa cache cho nhiều ngôn ngữ", description = "Xóa cache cho nhiều ngôn ngữ cùng lúc")
    @DeleteMapping("/cache/clear/languages")
    public ApiResponse<Object> clearCacheByLanguages(@RequestBody List<String> languages) {
        return i18nService.clearCacheByLanguages(languages);
    }

    @Operation(summary = "Load messages từ properties file", description = "Load messages từ file properties vào memory")
    @PostMapping("/load-from-properties")
    public ApiResponse<Object> loadFromProperties(
            @RequestParam String language) {
        return propertiesLoaderService.loadPropertiesFile(language);
    }

    @Operation(summary = "Load properties vào database", description = "Load messages từ file properties vào database")
    @PostMapping("/load-properties-to-db")
    public ApiResponse<Object> loadPropertiesToDatabase(
            @RequestParam String language) {
        return propertiesLoaderService.loadPropertiesToDatabase(language);
    }

    @Operation(summary = "Load database vào cache", description = "Load tất cả messages từ database vào Redis cache")
    @PostMapping("/load-db-to-cache")
    public ApiResponse<Object> loadDatabaseToCache(
            @RequestParam String language) {
        return i18nService.loadDatabaseToCache(language);
    }

    @Operation(summary = "Refresh toàn bộ cache", description = "Xóa cache hiện tại và load lại từ database")
    @PostMapping("/refresh-cache")
    public ApiResponse<Object> refreshCache() {
        return i18nService.refreshCache();
    }

    @Operation(summary = "Tạo bảng i18n", description = "Tạo bảng i18n_{language} trong database")
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

    @Operation(summary = "Debug: Xem Redis keys", description = "API debug để xem các keys trong Redis")
    @GetMapping("/debug/keys")
    public ApiResponse<Object> getRedisKeys(@RequestParam(required = false) String pattern) {
        return i18nService.getRedisKeys(pattern != null ? pattern : "i18n:*");
    }

    @Operation(summary = "Lấy messages dạng tree từ database", description = "Lấy messages theo cấu trúc cây từ database với pagination")
    @GetMapping("/messages/tree/db")
    public ApiResponse<I18nPageResponse> getMessagesTreeFromDb(
            @RequestParam String language,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String searchKey,
            @RequestParam(required = false) String searchMessage) {
        return i18nService.getMessagesTreeFromDb(language, page, size, searchKey, searchMessage);
    }

    @Operation(summary = "Lấy messages dạng tree từ Redis", description = "Lấy messages theo cấu trúc cây từ Redis cache với pagination")
    @GetMapping("/messages/tree/redis")
    public ApiResponse<I18nPageResponse> getMessagesTreeFromRedis(
            @RequestParam String language,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String searchKey,
            @RequestParam(required = false) String searchMessage) {
        return i18nService.getMessagesTreeFromRedis(language, page, size, searchKey, searchMessage);
    }

    @Operation(summary = "Tìm kiếm messages", description = "Tìm kiếm messages với khả năng chọn nguồn (db hoặc redis)")
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
