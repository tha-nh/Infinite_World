//package com.infinite.user.config;
//
//import com.fasterxml.jackson.annotation.JsonTypeInfo;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//import org.springframework.data.redis.cache.RedisCacheManager;
//import org.springframework.data.redis.connection.RedisClusterConfiguration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.RedisSerializationContext;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//import java.time.Duration;
//import java.util.List;
//
//@Configuration
//@EnableCaching
//public class RedisConfig {
//
//    @Value("${spring.data.redis.isAuthen}")
//    private String isAuthen;
//
//    @Value("${spring.data.redis.host}")
//    private String redisHost;
//
//    @Value("${spring.data.redis.port}")
//    private int redisPort;
//
//    @Value("${spring.data.redis.cluster.nodes}")
//    private List<String> redisNodes;
//
//    @Value("${spring.data.redis.cluster.username}")
//    private String username;
//
//    @Value("${spring.data.redis.cluster.password}")
//    private String password;
//
//    @Value("${spring.data.redis.cluster.max-redirects}")
//    private Integer maxRedirects;
//
//    @Bean
//    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        objectMapper.activateDefaultTyping(
//                LaissezFaireSubTypeValidator.instance,
//                ObjectMapper.DefaultTyping.NON_FINAL,
//                JsonTypeInfo.As.PROPERTY
//        );
//        RedisCacheConfiguration defaultConfig =
//                RedisCacheConfiguration.defaultCacheConfig()
//                        .disableCachingNullValues()
//                        .entryTtl(Duration.ofHours(24)) // TTL mặc định
//                        .serializeKeysWith(
//                                RedisSerializationContext.SerializationPair
//                                        .fromSerializer(new StringRedisSerializer())
//                        )
//                        .serializeValuesWith(
//                                RedisSerializationContext.SerializationPair
//                                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper))
//                        );
//
//        return RedisCacheManager.builder(factory)
//                .cacheDefaults(defaultConfig)
//                .build();
//    }
//
//    @Bean
//    public LettuceConnectionFactory redisConnectionFactory() {
//        if ("true".equals(this.isAuthen)) {
//            RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(redisNodes);
//            redisClusterConfiguration.setPassword(password);
//            redisClusterConfiguration.setMaxRedirects(maxRedirects);
//            return new LettuceConnectionFactory(redisClusterConfiguration);
//        }
//        var conn = new RedisStandaloneConfiguration(redisHost, redisPort);
//        conn.setUsername(username);
//        conn.setPassword(password);
//        return new LettuceConnectionFactory(conn);
//    }
//
//    @Bean
//    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
//        RedisTemplate<Object, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(redisConnectionFactory);
//        return template;
//    }
//
//    @Bean(name = "redisTemplateSerializer")
//    @Primary
//    public RedisTemplate<Object, Object> redisTemplateSerializer(RedisConnectionFactory redisConnectionFactory) {
//        RedisTemplate<Object, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(redisConnectionFactory);
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(new StringRedisSerializer());
//        return template;
//    }
//}
