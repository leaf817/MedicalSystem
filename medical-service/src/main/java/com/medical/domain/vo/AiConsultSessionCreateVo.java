package com.medical.domain.vo;

import lombok.Data;

@Data
public class AiConsultSessionCreateVo {
    private Long sessionId;
    private String sessionNo;
    private String welcomeMessage;
    private String disclaimer;
}
