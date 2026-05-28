package com.medical.ai.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.ai.client.dto.LlmChatMessage;
import com.medical.ai.client.dto.LlmChatResult;
import com.medical.ai.config.AiProperties;
import com.medical.ai.exception.LlmApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiCompatibleClientTest {

    private MockRestServiceServer server;
    private OpenAiCompatibleClient client;

    @BeforeEach
    void setUp() {
        AiProperties props = new AiProperties();
        props.setApiKey("test-api-key");
        props.setModel("glm-4-flash");
        props.setMaxOutputTokens(256);
        props.setTemperature(0.5);
        props.setTimeoutMs(10000);

        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiCompatibleClient(props, new ObjectMapper(), builder.build());
    }

    @AfterEach
    void verify() {
        server.verify();
    }

    @Test
    void chatParsesSuccessfulResponse() {
        String body = """
                {
                  "model": "glm-4-flash",
                  "choices": [{
                    "message": {
                      "role": "assistant",
                      "content": "请问头痛持续多久了？"
                    }
                  }],
                  "usage": {
                    "prompt_tokens": 12,
                    "completion_tokens": 8,
                    "total_tokens": 20
                  }
                }
                """;
        server.expect(requestTo("http://localhost/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        LlmChatResult result = client.chat(List.of(
                LlmChatMessage.system("你是问诊助手"),
                LlmChatMessage.user("我头痛")
        ));

        assertEquals("请问头痛持续多久了？", result.getContent());
        assertEquals("glm-4-flash", result.getModel());
        assertEquals(12, result.getPromptTokens());
        assertEquals(8, result.getCompletionTokens());
        assertFalse(result.isMock());
    }

    @Test
    void chatMaps401ToFriendlyMessage() {
        server.expect(requestTo("http://localhost/chat/completions"))
                .andRespond(withBadRequest().body("""
                        {"error":{"message":"invalid api key","type":"invalid_request_error"}}
                        """));

        LlmApiException ex = assertThrows(LlmApiException.class, () ->
                client.chat(List.of(LlmChatMessage.user("你好"))));
        assertTrue(ex.getMessage().contains("大模型"));
    }

    @Test
    void chatRequiresApiKey() {
        AiProperties props = new AiProperties();
        props.setApiKey("");
        OpenAiCompatibleClient noKeyClient =
                new OpenAiCompatibleClient(props, new ObjectMapper(), RestClient.builder().build());

        LlmApiException ex = assertThrows(LlmApiException.class, () ->
                noKeyClient.chat(List.of(LlmChatMessage.user("你好"))));
        assertTrue(ex.getMessage().contains("API Key"));
    }
}
