package com.medical.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentVo {
    private Long paymentId;
    private String paymentNo;
    private Long patientId;
    private String patientName;
    private String bizType;
    private String bizTypeText;
    private Long bizId;
    private String bizNo;
    private BigDecimal amount;
    private String payMethod;
    private String payMethodText;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;
    private Long operatorId;
    private String operatorName;
    private Integer status;
    private String statusText;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
