package com.infinite.common.util;

import lombok.RequiredArgsConstructor;
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
        try {
            I18nRedisClient redisClient = SpringContextHolder.getBean(I18nRedisClient.class);
            return redisClient.getMessage(key, language);
        } catch (IllegalStateException e) {
            // ApplicationContext not initialized yet, return key
            return key;
        } catch (Exception e) {
            return key;
        }
    }
}
