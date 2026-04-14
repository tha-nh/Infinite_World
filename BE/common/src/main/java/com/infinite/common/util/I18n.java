package com.infinite.common.util;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class I18n {
    private static MessageSource messageSource;

    public I18n(MessageSource messageSource) {
        I18n.messageSource = messageSource;
    }

    public static String msg(String key) {
        try {
            String message = messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
            return normalizeEncoding(message);
        } catch (NoSuchMessageException ex) {
            return normalizeEncoding(key);
        }
    }

    private static String normalizeEncoding(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String normalized = value;
        for (int i = 0; i < 3; i++) {
            if (!looksMisencoded(normalized)) {
                return normalized;
            }

            String converted = new String(normalized.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            if (converted.isBlank() || converted.equals(normalized)) {
                return normalized;
            }
            normalized = converted;
        }

        return normalized;
    }

    private static boolean looksMisencoded(String value) {
        return value.contains("Ã")
                || value.contains("Â")
                || value.contains("Ä")
                || value.contains("áº")
                || value.contains("á»")
                || value.contains("?")
                || value.contains("�");
    }
}
