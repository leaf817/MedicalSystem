package com.medical.domain.vo;

import lombok.Data;

@Data
public class ReceptionPatientVo {
    private Long patientId;
    private Long userId;
    private String patientNo;
    private String name;
    private String idCard;
    private String gender;
    private String phone;
    private Integer status;
}
