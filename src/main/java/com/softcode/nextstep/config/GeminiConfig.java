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
        String apiKey = properties.getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            apiKey = System.getenv("GOOGLE_API_KEY");
        }
        if (StringUtils.hasText(apiKey)) {
            builder = builder.apiKey(apiKey);
        }
        return builder.build();
    }
}
