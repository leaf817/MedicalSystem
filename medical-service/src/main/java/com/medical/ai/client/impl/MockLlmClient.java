package com.medical.ai.client.impl;

import com.medical.ai.client.LlmClient;
import com.medical.ai.client.dto.LlmChatMessage;
import com.medical.ai.client.dto.LlmChatResult;
import com.medical.ai.config.AiProperties;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 未启用真实模型时的 Mock 回复（答辩/无 Key 环境）
 */
@RequiredArgsConstructor
public class MockLlmClient implements LlmClient {

    private final AiProperties properties;

    @Override
    public LlmChatResult chat(List<LlmChatMessage> messages) {
        String lastUser = "";
        if (messages != null) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                LlmChatMessage m = messages.get(i);
                if ("user".equals(m.getRole()) && m.getContent() != null) {
                    lastUser = m.getContent().trim();
                    break;
                }
            }
        }

        String reply = "【模拟回复】我已收到您的描述"
                + (lastUser.isEmpty() ? "。" : "：「" + abbreviate(lastUser, 40) + "」。")
                + "请继续说明症状持续了多久、严重程度，以及是否有发热等伴随症状。"
                + "（当前为演示模式，未调用真实大模型）";

        return LlmChatResult.builder()
                .content(reply)
                .model(properties.getModel() + "-mock")
                .promptTokens(0)
                .completionTokens(0)
                .totalTokens(0)
                .mock(true)
                .build();
    }

    private static String abbreviate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "…";
    }
}
