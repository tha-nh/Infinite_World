package com.infinite.common.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MessageUtils {
    private static MessageSource messageSource;
    
    public MessageUtils(MessageSource messageSource) {
        MessageUtils.messageSource = messageSource;
    }
    
    public static String getMessage(String key, Object... args) {
        if (messageSource == null) {
            // Return key if MessageSource not initialized yet
            return key;
        }
        try {
            return messageSource.getMessage(
                    key,
                    args,
                    LocaleContextHolder.getLocale()
            );
        } catch (Exception e) {
            // Return key if message not found
            return key;
        }
    }
}