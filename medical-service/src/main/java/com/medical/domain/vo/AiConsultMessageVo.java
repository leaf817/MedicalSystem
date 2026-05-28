package com.medical.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiConsultMessageVo {
    private Long messageId;
    private Long sessionId;
    private String role;
    private String content;
    private String contentType;
    private String modelName;
    private LocalDateTime createdTime;
}
