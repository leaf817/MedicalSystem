package com.medical.domain.vo;

import lombok.Data;

@Data
public class DeptEpidemicHintVo {
    private Long deptId;
    private String deptName;
    private String hintType;
    private String hintTypeLabel;
    private String message;
    private Integer confidence;
    private String confidenceLabel;
}
