package com.infinite.core;

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

@SpringBootApplication(scanBasePackages = {"com.infinite.common", "com.infinite.core"})
public class CoreServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(CoreServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CoreServiceApplication.class);
        Environment env = app.run(args).getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = Optional.ofNullable(env.getProperty("server.ssl.key-store"))
                .map(key -> "https")
                .orElse("http");

        String serverPort = env.getProperty("server.port");

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
        String[] profiles = env.getActiveProfiles();

        String apiDocsEnabled = env.getProperty("springdoc.api-docs.enabled", "false");
        if ("true".equalsIgnoreCase(apiDocsEnabled)) {
            String[] newProfiles = Arrays.copyOf(profiles, profiles.length + 1);
            newProfiles[newProfiles.length - 1] = "api-docs";
            profiles = newProfiles;
        }
        System.out.println(
                "\n----------------------------------------------------------\n\t" +
                        "🚀 Application '" + env.getProperty("spring.application.name") + "' is running! Access URLs:\n\t" +
                        "🌐 Local:      " + protocol + "://localhost:" + serverPort + contextPath + "\n\t" +
                        "🌍 External:   " + protocol + "://" + hostAddress + ":" + serverPort + contextPath + "\n\t" +
                        "🧪 Profile(s): " + Arrays.toString(profiles) + "\n" +
                        "----------------------------------------------------------"
        );
    }
}