package com.medical.domain.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DoctorScheduleQueryDTO {

    private Long doctorId;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer status;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
