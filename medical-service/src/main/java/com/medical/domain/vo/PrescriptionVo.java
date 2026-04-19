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
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private List<PrescriptionDetailVo> details;
}