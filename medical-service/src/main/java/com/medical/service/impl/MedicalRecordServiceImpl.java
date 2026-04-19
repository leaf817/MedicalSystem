// medical-service/src/main/java/com/medical/service/impl/MedicalRecordServiceImpl.java
package com.medical.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.domain.dto.MedicalRecordSaveDto;
import com.medical.domain.entity.Doctor;
import com.medical.domain.entity.MedicalRecord;
import com.medical.domain.entity.Patient;
import com.medical.domain.vo.MedicalRecordVo;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.MedicalRecordMapper;
import com.medical.mapper.PatientMapper;
import com.medical.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordMapper medicalRecordMapper;
    private final PatientMapper patientMapper;
    private final DoctorMapper doctorMapper;

    @Override
    public List<MedicalRecordVo> getDoctorRecords(Long doctorId) {
        LambdaQueryWrapper<MedicalRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MedicalRecord::getDoctorId, doctorId)
                .orderByDesc(MedicalRecord::getVisitDate);

        List<MedicalRecord> records = medicalRecordMapper.selectList(wrapper);
        return records.stream()
                .map(this::buildMedicalRecordVo)
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalRecordVo> getPatientHistory(Long patientId) {
        LambdaQueryWrapper<MedicalRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MedicalRecord::getPatientId, patientId)
                .orderByDesc(MedicalRecord::getVisitDate);

        List<MedicalRecord> records = medicalRecordMapper.selectList(wrapper);
        return records.stream()
                .map(this::buildMedicalRecordVo)
                .collect(Collectors.toList());
    }

    @Override
    public MedicalRecordVo getRecordDetail(Long recordId) {
        MedicalRecord record = medicalRecordMapper.selectById(recordId);
        if (record == null) {
            return null;
        }
        return buildMedicalRecordVo(record);
    }

    @Override
    @Transactional
    public MedicalRecordVo saveRecord(MedicalRecordSaveDto dto, Long doctorId, String doctorName) {
        MedicalRecord record;

        if (dto.getRecordId() == null) {
            // 新增
            record = new MedicalRecord();
            BeanUtils.copyProperties(dto, record);
            record.setDoctorId(doctorId);
            record.setCreatedBy(doctorName);
            record.setUpdatedBy(doctorName);
            record.setCreatedTime(LocalDateTime.now());
            record.setUpdatedTime(LocalDateTime.now());
            record.setStatus(2); // 默认归档

            // 生成病历号: R + 年月日时分秒 + 4位随机数
            String recordNo = "R" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                    IdUtil.fastSimpleUUID().substring(0, 4).toUpperCase();
            record.setRecordNo(recordNo);

            medicalRecordMapper.insert(record);
        } else {
            // 更新
            record = medicalRecordMapper.selectById(dto.getRecordId());
            if (record != null) {
                BeanUtils.copyProperties(dto, record);
                record.setUpdatedBy(doctorName);
                record.setUpdatedTime(LocalDateTime.now());
                medicalRecordMapper.updateById(record);
            }
        }

        return buildMedicalRecordVo(record);
    }

    @Override
    @Transactional
    public boolean deleteRecord(Long recordId, Long doctorId) {
        LambdaQueryWrapper<MedicalRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MedicalRecord::getRecordId, recordId)
                .eq(MedicalRecord::getDoctorId, doctorId);
        return medicalRecordMapper.delete(wrapper) > 0;
    }

    /**
     * 构建 MedicalRecordVo
     */
    private MedicalRecordVo buildMedicalRecordVo(MedicalRecord record) {
        MedicalRecordVo vo = new MedicalRecordVo();
        BeanUtils.copyProperties(record, vo);

        if (record.getStatus() != null) {
            vo.setStatusText(record.getStatus() == 1 ? "草稿" : "已归档");
        }

        if (record.getPatientId() != null) {
            Patient patient = patientMapper.selectById(record.getPatientId());
            if (patient != null) {
                vo.setPatientName(patient.getName());
                vo.setPatientNo(patient.getPatientNo());
                vo.setPatientPhone(patient.getPhone());
                vo.setPatientBirthDate(patient.getBirthDate());
                vo.setPatientGender(patient.getGender());
            }
        }

        if (record.getDoctorId() != null) {
            Doctor doctor = doctorMapper.selectById(record.getDoctorId());
            if (doctor != null) {
                vo.setDoctorName(doctor.getName());
                vo.setDoctorTitle(doctor.getTitle());
            }
        }

        return vo;
    }
}