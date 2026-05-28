package com.medical.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MedicineStockAdjustDto {
    /** 实盘数量（盘点后按此值校正账面库存） */
    @NotNull(message = "实盘数量不能为空")
    @Min(value = 0, message = "实盘数量不能为负数")
    private Integer actualQuantity;

    private String remark;
}
