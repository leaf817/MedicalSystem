package com.medical.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dept_registration_alert_rule")
public class DeptRegistrationAlertRule {
    @TableId(type = IdType.AUTO)
    private Long ruleId;
    private Long deptId;
    private String ruleName;
    /** 1=今日挂号绝对值 2=较7日均值增幅(%) 3=明日预约绝对值 4=过去一周内预约合计(近7日含今日) */
    private Integer thresholdType;
    private BigDecimal thresholdValue;
    /** 1=提示 2=警告 3=严重 */
    private Integer alertLevel;
    private Integer enabled;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
