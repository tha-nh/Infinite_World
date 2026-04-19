package com.infinite.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinite.user.client.FileClient;
import com.infinite.user.client.impl.FileClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class FileClientConfig {
    
    @Bean
    public RestTemplate fileRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        
        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
    
    @Bean
    public FileClient fileClient(@Value("${file.service.url:http://localhost:8083}") String fileServiceUrl) {
        return new FileClientImpl(fileRestTemplate(), new ObjectMapper(), fileServiceUrl);
    }
}