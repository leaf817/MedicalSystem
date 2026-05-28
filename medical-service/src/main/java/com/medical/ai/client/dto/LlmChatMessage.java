package com.medical.ai.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI 兼容 chat messages 条目
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatMessage {

    private String role;
    private String content;

    public static LlmChatMessage system(String content) {
        return new LlmChatMessage("system", content);
    }

    public static LlmChatMessage user(String content) {
        return new LlmChatMessage("user", content);
    }

    public static LlmChatMessage assistant(String content) {
        return new LlmChatMessage("assistant", content);
    }
}
