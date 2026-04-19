package com.infinite.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.infinite.common.dto.event.AccountVerificationEvent;
import com.infinite.common.dto.event.EmailNotificationEvent;
import com.infinite.common.dto.event.UserStatusChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer Configuration
 * Only active when messaging.provider=kafka
 */
@Slf4j
@EnableKafka
@Configuration
@ConditionalOnProperty(name = "messaging.provider", havingValue = "kafka", matchIfMissing = true)
public class KafkaConsumerConfig {
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id:notification-service}")
    private String groupId;
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
    
    private Map<String, Object> getBaseConsumerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return config;
    }
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = getBaseConsumerConfig();
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EmailNotificationEvent.class.getName());
        
        log.info("Kafka Consumer configured with bootstrap servers: {}, group: {}", bootstrapServers, groupId);
        
        return new DefaultKafkaConsumerFactory<>(config, 
            new StringDeserializer(),
            new ErrorHandlingDeserializer<>(new JsonDeserializer<>(objectMapper())));
    }
    
    @Bean
    public ConsumerFactory<String, Object> accountVerificationConsumerFactory() {
        Map<String, Object> config = getBaseConsumerConfig();
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AccountVerificationEvent.class.getName());
        
        return new DefaultKafkaConsumerFactory<>(config, 
            new StringDeserializer(),
            new ErrorHandlingDeserializer<>(new JsonDeserializer<>(objectMapper())));
    }
    
    @Bean
    public ConsumerFactory<String, Object> userStatusChangeConsumerFactory() {
        Map<String, Object> config = getBaseConsumerConfig();
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, UserStatusChangeEvent.class.getName());
        
        return new DefaultKafkaConsumerFactory<>(config, 
            new StringDeserializer(),
            new ErrorHandlingDeserializer<>(new JsonDeserializer<>(objectMapper())));
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> accountVerificationListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(accountVerificationConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> userStatusChangeListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userStatusChangeConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
