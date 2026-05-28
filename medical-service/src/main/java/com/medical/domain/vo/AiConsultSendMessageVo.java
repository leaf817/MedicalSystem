package com.medical.domain.vo;

import lombok.Data;

@Data
public class AiConsultSendMessageVo {
    private Long userMessageId;
    private Long assistantMessageId;
    private String reply;
    /** 急症提示：EMERGENCY 等 */
    private String urgencyHint;
}
