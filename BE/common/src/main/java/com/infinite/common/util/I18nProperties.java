package com.infinite.common.util;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class I18nProperties {
    private static MessageSource messageSource;

    public I18nProperties(MessageSource messageSource) {
        I18nProperties.messageSource = messageSource;
    }

    public static String msg(String key) {
        try {
            return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException ex) {
            return key;
        }
    }

    public static String msg(String key, String language) {
        try {
            java.util.Locale locale = new java.util.Locale(language);
            return messageSource.getMessage(key, null, locale);
        } catch (NoSuchMessageException ex) {
            return key;
        }
    }
}
