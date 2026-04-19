package com.medical.domain.vo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DoctorScheduleVO {

    private Long scheduleId;

    private Long doctorId;

    private String doctorName;

    private String deptName;

    private LocalDate scheduleDate;

    private String timeSlot;

    private Integer totalSlots;

    private Integer bookedSlots;

    private Integer remainingSlots;

    private Integer status;

    private String statusText;

    private String remark;
}