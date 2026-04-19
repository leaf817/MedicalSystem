package com.medical.domain;

import lombok.Data;

@Data
public class ScheduleQueryParam {
    private Long deptId;
    private Long doctorId;
    private Integer status;
    private String startDate;
    private String endDate;
    private Long current = 1L;
    private Long size = 10L;
}