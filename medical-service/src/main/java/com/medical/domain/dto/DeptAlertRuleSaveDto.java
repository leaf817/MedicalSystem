package com.medical.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeptAlertRuleSaveDto {
    @NotNull(message = "请选择科室")
    private Long deptId;

    private String ruleName;

    @NotNull(message = "请选择阈值类型")
    @Min(value = 1, message = "阈值类型无效")
    @Max(value = 4, message = "阈值类型无效")
    private Integer thresholdType;

    @NotNull(message = "请填写阈值")
    @DecimalMin(value = "0", inclusive = false, message = "阈值必须大于0")
    private BigDecimal thresholdValue;

    @NotNull(message = "请选择预警级别")
    @Min(value = 1, message = "预警级别无效")
    @Max(value = 3, message = "预警级别无效")
    private Integer alertLevel;

    private Integer enabled;
    private String remark;
}
