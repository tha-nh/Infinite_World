package com.infinite.notification.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnreadCountCacheService {

    private static final String KEY_PREFIX = "notification:unread:";
    private static final Duration TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;

    public Optional<Long> get(Long userId) {
        try {
            String value = redisTemplate.opsForValue().get(key(userId));
            return value == null ? Optional.empty() : Optional.of(Long.parseLong(value));
        } catch (Exception ex) {
            log.warn("Unread cache get failed: userId={}", userId, ex);
            return Optional.empty();
        }
    }

    public void set(Long userId, long count) {
        try {
            redisTemplate.opsForValue().set(key(userId), String.valueOf(Math.max(count, 0)), TTL);
        } catch (Exception ex) {
            log.warn("Unread cache set failed: userId={}, count={}", userId, count, ex);
        }
    }

    public void increment(Long userId, long delta) {
        try {
            redisTemplate.opsForValue().increment(key(userId), delta);
            redisTemplate.expire(key(userId), TTL);
        } catch (Exception ex) {
            log.warn("Unread cache increment failed: userId={}, delta={}", userId, delta, ex);
        }
    }

    public void evict(Long userId) {
        try {
            redisTemplate.delete(key(userId));
        } catch (Exception ex) {
            log.warn("Unread cache evict failed: userId={}", userId, ex);
        }
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
