package com.medical.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MedicineStockLogVo {
    private Long logId;
    private Long medicineId;
    private String medicineCode;
    private String medicineName;
    private Integer changeQty;
    private Integer beforeQty;
    private Integer afterQty;
    private String bizType;
    private String bizTypeText;
    private Long bizId;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime createdTime;
}
