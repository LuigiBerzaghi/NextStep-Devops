package com.softcode.nextstep.config;

import com.google.genai.Client;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class GeminiTestConfig {

    @Bean(destroyMethod = "close")
    @Primary
    Client geminiClient() {
        // retorna um client básico que não faz chamadas externas durante os testes
        return Client.builder().apiKey("test-key").build();
    }
}
