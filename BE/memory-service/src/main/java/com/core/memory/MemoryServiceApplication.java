package com.core.memory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;

@SpringBootApplication
public class MemoryServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(MemoryServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MemoryServiceApplication.class);
        Environment env = app.run(args).getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = Optional.ofNullable(env.getProperty("server.ssl.key-store"))
                .map(key -> "https")
                .orElse("http");

        String serverPort = env.getProperty("server.port", "8080");

        String contextPath = Optional
                .ofNullable(env.getProperty("server.servlet.context-path"))
                .filter(StringUtils::isNotBlank)
                .orElse("/");

        String hostAddress = "localhost";

        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Cannot determine host address, using localhost");
        }

        System.out.println(
                "\n----------------------------------------------------------\n\t" +
                        "🚀 Application '" + env.getProperty("spring.application.name") + "' is running!\n\t" +
                        "🌐 Local:      " + protocol + "://localhost:" + serverPort + contextPath + "\n\t" +
                        "🌍 External:   " + protocol + "://" + hostAddress + ":" + serverPort + contextPath + "\n\t" +
                        "🧪 Profile(s): " + Arrays.toString(
                        env.getActiveProfiles().length == 0
                                ? env.getDefaultProfiles()
                                : env.getActiveProfiles()
                ) + "\n" +
                        "----------------------------------------------------------"
        );
    }
}