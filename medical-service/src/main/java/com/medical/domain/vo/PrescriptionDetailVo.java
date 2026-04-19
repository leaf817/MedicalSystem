package com.medical.domain.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PrescriptionDetailVo {
    private Long detailId;
    private Long medicineId;
    private String medicineName;
    private String medicineCode;
    private String spec;
    private String unit;
    private Integer quantity;
    private String dosage;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime createdTime;
}