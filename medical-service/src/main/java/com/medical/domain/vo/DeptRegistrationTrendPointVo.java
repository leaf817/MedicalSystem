package com.medical.domain.vo;

import lombok.Data;

@Data
public class DeptRegistrationTrendPointVo {
    private String date;
    private Long count;
    /** past / today / future */
    private String periodType;
    private String periodTypeLabel;
}
