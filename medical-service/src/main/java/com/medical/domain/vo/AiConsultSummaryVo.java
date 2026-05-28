package com.medical.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiConsultSummaryVo {
    private Long sessionId;
    private String chiefComplaint;
    private String urgencyLevel;
    private Long suggestedDeptId;
    private String suggestedDeptName;
    private String medicalAdvice;
    private List<String> questionsForDoctor;
    /** 完整 JSON 或原文 */
    private String rawSummary;
}
