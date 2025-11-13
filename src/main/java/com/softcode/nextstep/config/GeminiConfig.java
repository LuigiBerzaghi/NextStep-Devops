package com.softcode.nextstep.config;

import com.google.genai.Client;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    @Bean(destroyMethod = "close")
    Client geminiClient(GeminiProperties properties) {
        Client.Builder builder = Client.builder();
        if (StringUtils.hasText(properties.getApiKey())) {
            builder = builder.apiKey(properties.getApiKey());
        }
        return builder.build();
    }
}

