package com.medical.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class PrescriptionUpdateDto {
    private List<PrescriptionDetailDto> details;
    private String remark;

    @Data
    public static class PrescriptionDetailDto {
        private Long detailId;
        private Long medicineId;
        private Integer quantity;
        private String dosage;
        private String remark;
    }
}