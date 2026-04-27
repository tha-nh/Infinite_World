package com.infinite.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class I18nRedisClient {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${i18n.cache-prefix:i18n:}")
    private String cachePrefix;

    public String getMessage(String messageKey, String language) {
        try {
            String redisKey = cachePrefix + language + ":" + messageKey;
            String message = redisTemplate.opsForValue().get(redisKey);

            if (message != null) {
                return message;
            }

            return messageKey;
        } catch (Exception e) {
            return messageKey;
        }
    }

    public String getMessageWithFallback(String messageKey, String language, String defaultLanguage) {
        String message = getMessage(messageKey, language);
        if (message.equals(messageKey) && !language.equals(defaultLanguage)) {
            message = getMessage(messageKey, defaultLanguage);
        }
        return message;
    }
}
