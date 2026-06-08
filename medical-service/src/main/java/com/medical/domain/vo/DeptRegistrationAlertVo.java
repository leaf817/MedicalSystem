package com.medical.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeptRegistrationAlertVo {
    private Long ruleId;
    private Long deptId;
    private String deptName;
    private String ruleName;
    /** 1=提示 2=警告 3=严重 */
    private Integer alertLevel;
    private String alertLevelLabel;
    private String message;
    private BigDecimal currentValue;
    private BigDecimal thresholdValue;
    private Integer thresholdType;
}
