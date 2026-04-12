package com.project.core.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Constant {
    public static String SERVICE_NAME;

    @Value("${spring.application.name:unknown-service}")
    public void setServiceName(String serviceName) {
        Constant.SERVICE_NAME = serviceName;
    }

}
