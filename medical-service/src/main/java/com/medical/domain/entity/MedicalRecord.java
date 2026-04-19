package com.medical.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    private Integer status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private String createdBy;

    private String updatedBy;
}