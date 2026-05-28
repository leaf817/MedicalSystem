package com.medical.ai.client;

import com.medical.ai.client.dto.LlmChatMessage;
import com.medical.ai.client.dto.LlmChatResult;

import java.util.List;

/**
 * 大模型对话客户端（OpenAI Chat Completions 兼容）
 */
public interface LlmClient {

    /**
     * 发送多轮消息并获取助手回复
     */
    LlmChatResult chat(List<LlmChatMessage> messages);
}
