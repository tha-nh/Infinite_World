package com.infinite.user;

import com.infinite.user.config.AsyncSyncConfiguration;
import com.infinite.user.config.EmbeddedElasticsearch;
import com.infinite.user.config.EmbeddedRedis;
import com.infinite.user.config.EmbeddedSQL;
import com.infinite.user.config.JacksonConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = { UserServiceApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class })
@EmbeddedRedis
@EmbeddedElasticsearch
@EmbeddedSQL
public @interface IntegrationTest {
}
