package com.medical.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PrescriptionVo {
    private Long prescriptionId;
    private String prescriptionNo;
    private Long recordId;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String doctorTitle;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusText;
    /** 是否已缴费：0=未支付 1=已支付（以 payment 表为准） */
    private Integer paid;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private List<PrescriptionDetailVo> details;
}