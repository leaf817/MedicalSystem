package com.medical.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prescription_detail")
public class PrescriptionDetail {

    @TableId(type = IdType.AUTO)
    private Long detailId;

    private Long prescriptionId;

    private Long medicineId;

    private Integer quantity;

    private String dosage;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}