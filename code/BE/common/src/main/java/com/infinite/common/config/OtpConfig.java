package com.infinite.common.config;

import com.infinite.common.service.OtpService;
import com.infinite.common.service.impl.OtpServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class OtpConfig {
    
    @Bean
    @ConditionalOnMissingBean
    public OtpService otpService(RedisTemplate<String, String> redisTemplate) {
        return new OtpServiceImpl(redisTemplate);
    }
}