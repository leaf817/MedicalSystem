package com.medical.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReceptionAppointmentCreateDto {
    @NotNull(message = "患者不能为空")
    private Long patientId;

    @NotNull(message = "排班ID不能为空")
    private Long scheduleId;
}
