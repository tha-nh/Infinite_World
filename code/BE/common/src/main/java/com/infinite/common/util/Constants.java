package com.infinite.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Constants {
    public static String SERVICE_NAME;

    @Value("${spring.application.name}")
    public void setServiceName(String serviceName) {
        Constants.SERVICE_NAME = serviceName;
    }

    public static final String EMAIL_REGEX = "^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$";

}
