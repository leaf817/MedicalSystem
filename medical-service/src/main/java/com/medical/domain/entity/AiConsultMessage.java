package com.medical.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 智能问诊消息
 */
@Data
@TableName("ai_consult_message")
public class AiConsultMessage {

    @TableId(type = IdType.AUTO)
    private Long messageId;

    private Long sessionId;
    /** system / user / assistant */
    private String role;
    private String content;
    private String contentType;
    private String modelName;
    private Integer promptTokens;
    private Integer completionTokens;
    private LocalDateTime createdTime;
}
