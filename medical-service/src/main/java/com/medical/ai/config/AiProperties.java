package com.medical.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 大模型 / AI 问诊配置（前缀 medical.ai）
 */
@Data
@ConfigurationProperties(prefix = "medical.ai")
public class AiProperties {

    /** 是否启用真实模型调用；false 时走 Mock（步骤 3 实现） */
    private boolean enabled = true;

    /** 厂商标识：zhipu | dashscope | deepseek | openai */
    private String provider = "zhipu";

    /** OpenAI 兼容 API 根地址，需以 / 结尾 */
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4/";

    /** API Key，优先环境变量 ZHIPU_API_KEY */
    private String apiKey = "";

    private String model = "glm-4-flash";

    private int timeoutMs = 60000;

    /** 带入模型的历史消息条数上限（user+assistant） */
    private int maxContextMessages = 20;

    private int maxOutputTokens = 1024;

    private double temperature = 0.5;

    /** 每用户每日最大请求次数（步骤 4 限流使用） */
    private int dailyRequestLimitPerUser = 50;

    private int maxUserMessageLength = 2000;

    /** enabled=false 时是否返回 Mock 文案 */
    private boolean mockReplyEnabled = true;
}
