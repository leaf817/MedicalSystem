package com.medical.domain.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BatchScheduleDTO {
    private Long deptId;
    private Long doctorId;
    private LocalDate startDate;   // 开始日期
    private LocalDate endDate;     // 结束日期
    private String timeSlot;
    private Integer totalSlots;
    private String remark;
}