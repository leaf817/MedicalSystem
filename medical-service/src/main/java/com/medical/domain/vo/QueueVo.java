package com.medical.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QueueVo {
    private Long appointmentId;
    private String appointmentNo;
    private String appointmentDate;

    private Long patientId;
    private String patientName;
    private String patientNo;
    private String gender;
    private Integer age;
    private String phone;

    private Integer queueNo;
    private String timeSlot;
    private Integer status;
    private String statusText;
    private Integer paid;
    private BigDecimal feeAmount;
    private Boolean called;
    private Integer isCheckedIn;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}