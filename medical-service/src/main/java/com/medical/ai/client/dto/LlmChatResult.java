package com.medical.ai.client.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 大模型单次对话结果
 */
@Data
@Builder
public class LlmChatResult {

    private String content;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    /** 是否为本地 Mock，非真实模型 */
    private boolean mock;
}
