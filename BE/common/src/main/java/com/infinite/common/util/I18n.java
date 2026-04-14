package com.infinite.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class I18n {

    private final I18nRedisClient redisClient;

    public static String msg(String key) {
        String locale = LocaleContextHolder.getLocale().getLanguage();
        return msg(key, locale);
    }

    public static String msg(String key, String language) {
        I18nRedisClient redisClient = SpringContextHolder.getBean(I18nRedisClient.class);
        try {
            return redisClient.getMessage(key, language);
        } catch (Exception e) {
            return key;
        }
    }
}
