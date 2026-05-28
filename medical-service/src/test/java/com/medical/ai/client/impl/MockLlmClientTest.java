package com.medical.ai.client.impl;

import com.medical.ai.client.LlmClient;
import com.medical.ai.client.dto.LlmChatMessage;
import com.medical.ai.client.dto.LlmChatResult;
import com.medical.ai.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "medical.ai.enabled=false",
        "medical.ai.mock-reply-enabled=true"
})
class MockLlmClientTest {

    @Autowired
    private LlmClient llmClient;

    @Test
    void returnsMockReplyWhenDisabled() {
        LlmChatResult result = llmClient.chat(List.of(
                LlmChatMessage.user("我咳嗽两天了")
        ));
        assertTrue(result.isMock());
        assertTrue(result.getContent().contains("模拟回复"));
        assertTrue(result.getContent().contains("咳嗽"));
    }
}
