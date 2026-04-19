// medical-service/src/main/java/com/medical/domain/vo/MedicalRecordVo.java
package com.medical.domain.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MedicalRecordVo {
    private Long recordId;
    private String recordNo;
    private Long patientId;
    private String patientName;
    private String patientNo;
    private Integer patientGender;
    private Integer patientAge;
    private Long doctorId;
    private String doctorName;
    private String doctorTitle;
    private Long appointmentId;
    private String appointmentNo;
    private LocalDate visitDate;
    private String chiefComplaint;
    private String presentIllness;
    private String pastHistory;
    private String physicalExam;
    private String diagnosis;
    private String treatmentPlan;
    private String aiSuggestion;
    private Integer status;
    private String statusText;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}