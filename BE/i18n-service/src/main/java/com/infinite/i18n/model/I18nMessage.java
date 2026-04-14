package com.infinite.i18n.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base entity for i18n messages
 * Tables are dynamically created per language (i18n_en, i18n_vi, etc.)
 * with dynamic columns: id, key, message, key_1, key_2, ..., key_10, is_deleted
 * 
 * Example:
 * - i18n_en table: id, key, message, key_1 (user), key_2 (profile), key_3 (name), keys_4-10 (null), is_deleted
 * - i18n_vi table: id, key, message, key_1 (user), key_2 (profile), key_3 (name), keys_4-10 (null), is_deleted
 * 
 * Constraints:
 * - Minimum 1 key level
 * - Maximum 10 key levels
 * - key format: "level1.level2.level3...level10"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class I18nMessage {
    private Long id;
    private String key;                // e.g., "user.profile.name" or simply "SUCCESS"
    private String message;            // The actual translated message
    private String key1;               // First level of key split  
    private String key2;               // Second level of key split
    private String key3;               // Third level
    private String key4;               // Fourth level
    private String key5;               // Fifth level
    private String key6;               // Sixth level
    private String key7;               // Seventh level
    private String key8;               // Eighth level
    private String key9;               // Ninth level
    private String key10;              // Tenth level
    private Boolean isDeleted = false;  // Soft delete flag
    private String language;           // Language code (e.g., "en", "vi")
}
