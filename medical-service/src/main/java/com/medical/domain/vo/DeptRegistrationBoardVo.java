package com.medical.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class DeptRegistrationBoardVo {
    private List<DeptRegistrationStatVo> deptStats;
    private List<DeptRegistrationAlertVo> alerts;
    private List<DeptEpidemicHintVo> epidemicHints;
}
