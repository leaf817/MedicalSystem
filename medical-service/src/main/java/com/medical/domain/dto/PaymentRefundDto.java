package com.medical.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRefundDto {
    @NotNull(message = "支付记录不能为空")
    private Long paymentId;

    private String reason;
}
