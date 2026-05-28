package com.medical.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiConsultSessionDetailVo {
    private Long sessionId;
    private String sessionNo;
    private String title;
    private Integer status;
    private String urgencyLevel;
    private Integer messageCount;
    private LocalDateTime createdTime;
    private LocalDateTime endedTime;
    private List<AiConsultMessageVo> messages;
}
