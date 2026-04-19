// medical-service/src/main/java/com/medical/domain/dto/MedicalRecordSaveDto.java
package com.medical.domain.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MedicalRecordSaveDto {
    private Long recordId;
    private Long patientId;
    private LocalDate visitDate;
    private String chiefComplaint;
    private String presentIllness;
    private String pastHistory;
    private String physicalExam;
    private String diagnosis;
    private String treatmentPlan;
    private Integer status;
}