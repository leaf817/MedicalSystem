// medical-service/src/main/java/com/medical/service/MedicalRecordService.java
package com.medical.service;

import com.medical.domain.dto.MedicalRecordSaveDto;
import com.medical.domain.vo.MedicalRecordVo;
import java.util.List;

public interface MedicalRecordService {

    /**
     * 获取所有病历（当前医生）
     */
    List<MedicalRecordVo> getDoctorRecords(Long doctorId);

    /**
     * 获取患者历史病历
     */
    List<MedicalRecordVo> getPatientHistory(Long patientId);
    List<MedicalRecordVo> getPatientHistoryForDoctor(Long patientId, Long doctorId);

    /**
     * 获取病历详情
     */
    MedicalRecordVo getRecordDetail(Long recordId);
    MedicalRecordVo getRecordDetailForDoctor(Long recordId, Long doctorId);

    /**
     * 保存病历
     */
    MedicalRecordVo saveRecord(MedicalRecordSaveDto dto, Long doctorId, String doctorName);

    /**
     * 删除病历
     */
    boolean deleteRecord(Long recordId, Long doctorId);
}