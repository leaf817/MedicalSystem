package com.medical.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.ai.client.LlmClient;
import com.medical.ai.client.impl.MockLlmClient;
import com.medical.ai.client.impl.OpenAiCompatibleClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 按配置装配 LlmClient：enabled=true 走真实 API，否则 Mock
 */
@Configuration
public class AiClientConfiguration {

    @Bean
    public LlmClient llmClient(AiProperties properties, ObjectMapper objectMapper) {
        if (!properties.isEnabled()) {
            if (properties.isMockReplyEnabled()) {
                return new MockLlmClient(properties);
            }
            return messages -> {
                throw new com.medical.ai.exception.LlmApiException(
                        "大模型功能已关闭（medical.ai.enabled=false）");
            };
        }
        return new OpenAiCompatibleClient(properties, objectMapper);
    }
}
