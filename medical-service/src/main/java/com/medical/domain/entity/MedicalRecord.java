// medical-service/src/main/java/com/medical/domain/entity/MedicalRecord.java
package com.medical.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("medical_record")
public class MedicalRecord {

    @TableId(type = IdType.AUTO)
    private Long recordId;

    private String recordNo;

    private Long patientId;

    private Long doctorId;

    private Long appointmentId;

    private LocalDate visitDate;

    private String chiefComplaint;

    private String presentIllness;

    private String pastHistory;

    private String physicalExam;

    private String diagnosis;

    private String treatmentPlan;

    private String aiSuggestion;

    private Integer status; // 1=草稿 2=已归档

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}