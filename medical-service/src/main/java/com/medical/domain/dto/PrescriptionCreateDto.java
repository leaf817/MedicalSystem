package com.medical.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PrescriptionCreateDto {

    @NotNull(message = "病历ID不能为空")
    private Long recordId;

    @Size(min = 1, message = "至少需要一个药品")
    @Valid
    private List<PrescriptionDetailDto> details;

    private String remark;

    @Data
    public static class PrescriptionDetailDto {
        @NotNull(message = "药品ID不能为空")
        private Long medicineId;

        @NotNull(message = "数量不能为空")
        private Integer quantity;

        private String dosage;

        private String remark;
    }
}