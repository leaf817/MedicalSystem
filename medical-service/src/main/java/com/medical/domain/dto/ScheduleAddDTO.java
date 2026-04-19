package com.medical.domain.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ScheduleAddDTO {
    private Long deptId;
    private Long doctorId;
    private LocalDate scheduleDate;
    private String timeSlot;
    private Integer totalSlots;
    private Integer status;
    private String remark;
}