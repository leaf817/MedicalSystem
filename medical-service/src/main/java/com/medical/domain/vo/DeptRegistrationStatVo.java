package com.medical.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeptRegistrationStatVo {
    private Long deptId;
    private String deptName;
    private String deptCode;
    private Long todayCount;
    private Long periodCount;
    /** 近7日（不含今日）日均挂号量 */
    private BigDecimal avg7Days;
    /** 今日较7日均值变化百分比，无基线时为 null */
    private BigDecimal changePercent;
    /** 明日预约数 */
    private Long tomorrowCount;
    /** 后日预约数 */
    private Long dayAfterTomorrowCount;
}
