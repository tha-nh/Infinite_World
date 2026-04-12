package com.infinite.common.util;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class I18n {

    private static MessageSource messageSource;

    public I18n(MessageSource messageSource) {
        I18n.messageSource = messageSource;
    }

    public static String msg(String key) {
        return messageSource.getMessage(
                key,
                null,
                LocaleContextHolder.getLocale()
        );
    }
}