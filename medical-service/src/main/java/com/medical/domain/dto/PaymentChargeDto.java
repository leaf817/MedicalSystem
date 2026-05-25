package com.medical.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentChargeDto {
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    @NotNull(message = "业务ID不能为空")
    private Long bizId;

    @NotBlank(message = "支付方式不能为空")
    private String payMethod;

    private BigDecimal amount;

    private String remark;
}
