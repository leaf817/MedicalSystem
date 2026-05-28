package com.medical.ai.client.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.ai.client.LlmClient;
import com.medical.ai.client.dto.LlmChatMessage;
import com.medical.ai.client.dto.LlmChatResult;
import com.medical.ai.config.AiProperties;
import com.medical.ai.exception.LlmApiException;
import com.medical.ai.exception.LlmTimeoutException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智谱等 OpenAI 兼容 /chat/completions 实现
 */
@Slf4j
public class OpenAiCompatibleClient implements LlmClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiCompatibleClient(AiProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, buildRestClient(properties));
    }

    OpenAiCompatibleClient(AiProperties properties, ObjectMapper objectMapper, RestClient restClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    @Override
    public LlmChatResult chat(List<LlmChatMessage> messages) {
        validateApiKey();
        if (messages == null || messages.isEmpty()) {
            throw new LlmApiException("对话消息不能为空");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", messages.stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList()));
        body.put("max_tokens", properties.getMaxOutputTokens());
        body.put("temperature", properties.getTemperature());

        try {
            String responseJson = restClient.post()
                    .uri("chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return parseResponse(responseJson);
        } catch (RestClientResponseException e) {
            log.warn("LLM API error status={} body={}", e.getStatusCode(), truncate(e.getResponseBodyAsString()));
            throw new LlmApiException(mapHttpError(e), e);
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                throw new LlmTimeoutException("智能问诊服务响应超时，请稍后重试", e);
            }
            throw new LlmApiException("无法连接智能问诊服务，请检查网络", e);
        } catch (LlmApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM call failed", e);
            throw new LlmApiException("智能问诊服务暂时不可用，请稍后重试", e);
        }
    }

    private void validateApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new LlmApiException("未配置大模型 API Key，请设置环境变量 ZHIPU_API_KEY 或 application-local.yml");
        }
    }

    private LlmChatResult parseResponse(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            throw new LlmApiException("大模型返回为空");
        }
        try {
            ChatCompletionResponse response = objectMapper.readValue(responseJson, ChatCompletionResponse.class);
            if (response.getError() != null && response.getError().getMessage() != null) {
                throw new LlmApiException("大模型服务异常：" + response.getError().getMessage());
            }
            if (response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new LlmApiException("大模型未返回有效回复");
            }
            ChatCompletionResponse.Message message = response.getChoices().get(0).getMessage();
            if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                throw new LlmApiException("大模型回复内容为空");
            }

            LlmChatResult.LlmChatResultBuilder builder = LlmChatResult.builder()
                    .content(message.getContent().trim())
                    .model(response.getModel() != null ? response.getModel() : properties.getModel())
                    .mock(false);

            if (response.getUsage() != null) {
                builder.promptTokens(response.getUsage().getPromptTokens())
                        .completionTokens(response.getUsage().getCompletionTokens())
                        .totalTokens(response.getUsage().getTotalTokens());
            }
            return builder.build();
        } catch (LlmApiException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmApiException("解析大模型响应失败", e);
        }
    }

    private String mapHttpError(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 401 || status == 403) {
            return "大模型 API 鉴权失败，请检查 API Key 是否有效";
        }
        if (status == 429) {
            return "大模型调用过于频繁，请稍后再试";
        }
        if (status >= 500) {
            return "大模型服务繁忙，请稍后重试";
        }
        String body = e.getResponseBodyAsString();
        if (body != null && body.contains("error")) {
            try {
                ChatCompletionResponse err = objectMapper.readValue(body, ChatCompletionResponse.class);
                if (err.getError() != null && err.getError().getMessage() != null) {
                    return "大模型服务异常：" + err.getError().getMessage();
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        return "大模型服务异常（HTTP " + status + "）";
    }

    private static boolean isTimeout(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof SocketTimeoutException) {
                return true;
            }
            cur = cur.getCause();
        }
        String msg = e.getMessage();
        return msg != null && msg.toLowerCase().contains("timed out");
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }

    private static RestClient buildRestClient(AiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = Math.max(properties.getTimeoutMs(), 5000);
        factory.setConnectTimeout(Duration.ofMillis(timeout));
        factory.setReadTimeout(Duration.ofMillis(timeout));

        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://open.bigmodel.cn/api/paas/v4/";
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChatCompletionResponse {
        private String model;
        private List<Choice> choices;
        private Usage usage;
        private ApiError error;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Choice {
            private Message message;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Message {
            private String role;
            private String content;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Usage {
            @JsonProperty("prompt_tokens")
            private Integer promptTokens;
            @JsonProperty("completion_tokens")
            private Integer completionTokens;
            @JsonProperty("total_tokens")
            private Integer totalTokens;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class ApiError {
            private String message;
            private String type;
        }
    }
}
