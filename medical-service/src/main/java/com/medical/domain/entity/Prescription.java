package com.medical.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("prescription")
public class Prescription {

    @TableId(type = IdType.AUTO)
    private Long prescriptionId;

    private String prescriptionNo;

    private Long recordId;

    private Long patientId;

    private Long doctorId;

    private BigDecimal totalAmount;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    private String createdBy;

    private String updatedBy;
}