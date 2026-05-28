package com.medical.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiConsultMessageSendDto {

    @NotBlank(message = "消息内容不能为空")
    private String content;
}
