package com.goaiplatform.backend;

import com.goaiplatform.backend.config.AppProperties;
import com.goaiplatform.backend.config.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, RateLimitProperties.class})
public class GoAiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoAiBackendApplication.class, args);
    }
}
