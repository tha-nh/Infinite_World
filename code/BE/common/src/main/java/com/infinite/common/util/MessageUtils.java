package com.infinite.common.util;

import com.infinite.common.service.JsonMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Component
public class MessageUtils {
    private static JsonMessageSource jsonMessageSource;
    
    public MessageUtils(JsonMessageSource jsonMessageSource) {
        MessageUtils.jsonMessageSource = jsonMessageSource;
    }
    
    /**
     * Get message by key with optional arguments for formatting
     * Supports MessageFormat patterns like {0}, {1}, etc.
     */
    public static String getMessage(String key, Object... args) {
        if (jsonMessageSource == null) {
            // Return key if JsonMessageSource not initialized yet
            return key;
        }
        try {
            String message = jsonMessageSource.getMessage(
                    key,
                    LocaleContextHolder.getLocale()
            );
            
            // If arguments provided, format the message
            if (args != null && args.length > 0) {
                return MessageFormat.format(message, args);
            }
            
            return message;
        } catch (Exception e) {
            // Return key if message not found
            return key;
        }
    }
    
    /**
     * Get message by key and specific language
     */
    public static String getMessage(String key, String language, Object... args) {
        if (jsonMessageSource == null) {
            return key;
        }
        try {
            String message = jsonMessageSource.getMessage(key, language);
            
            // If arguments provided, format the message
            if (args != null && args.length > 0) {
                return MessageFormat.format(message, args);
            }
            
            return message;
        } catch (Exception e) {
            return key;
        }
    }
}