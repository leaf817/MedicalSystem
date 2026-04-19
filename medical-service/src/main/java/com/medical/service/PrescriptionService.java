package com.medical.service;

import com.medical.domain.dto.PrescriptionCreateDto;
import com.medical.domain.dto.PrescriptionUpdateDto;
import com.medical.domain.vo.PrescriptionVo;
import java.util.List;

public interface PrescriptionService {

    PrescriptionVo createPrescription(PrescriptionCreateDto dto, Long doctorId, String doctorName);

    PrescriptionVo updatePrescription(Long id, PrescriptionUpdateDto dto, Long doctorId);

    PrescriptionVo getPrescriptionDetail(Long id);

    List<PrescriptionVo> getPrescriptionsByRecordId(Long recordId);

    List<PrescriptionVo> getPrescriptionsByPatientId(Long patientId);

    void deletePrescription(Long id, Long doctorId);

    void dispensePrescription(Long id, Long nurseId);

    List<PrescriptionVo> getPendingDispenseList(String keyword, Integer status);

    List<PrescriptionVo> getPrescriptionsByDoctorId(Long doctorId, Integer status);
}