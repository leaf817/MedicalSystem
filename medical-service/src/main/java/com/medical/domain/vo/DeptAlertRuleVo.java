package com.medical.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DeptAlertRuleVo {
    private Long ruleId;
    private Long deptId;
    private String deptName;
    private String ruleName;
    private Integer thresholdType;
    private String thresholdTypeLabel;
    private BigDecimal thresholdValue;
    private Integer alertLevel;
    private String alertLevelLabel;
    private Integer enabled;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
