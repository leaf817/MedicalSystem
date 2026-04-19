package com.medical.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.exception.ServiceException;
import com.medical.domain.dto.PrescriptionCreateDto;
import com.medical.domain.dto.PrescriptionUpdateDto;
import com.medical.domain.entity.*;
import com.medical.domain.vo.PrescriptionDetailVo;
import com.medical.domain.vo.PrescriptionVo;
import com.medical.mapper.*;
import com.medical.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionDetailMapper prescriptionDetailMapper;
    private final MedicineMapper medicineMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final PatientMapper patientMapper;
    private final DoctorMapper doctorMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrescriptionVo createPrescription(PrescriptionCreateDto dto, Long doctorId, String doctorName) {
        // 验证病历
        MedicalRecord record = medicalRecordMapper.selectById(dto.getRecordId());
        if (record == null) {
            throw new BusinessWarningException("病历不存在");
        }
        if (!record.getDoctorId().equals(doctorId)) {
            throw new BusinessWarningException("只能为自己的病历开处方");
        }

        List<PrescriptionDetail> details = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PrescriptionCreateDto.PrescriptionDetailDto detailDto : dto.getDetails()) {
            Medicine medicine = requireMedicineForLine(detailDto.getMedicineId(), detailDto.getQuantity(), true);
            PrescriptionDetail detail = toDetailEntity(medicine, detailDto.getQuantity(), detailDto.getDosage(), detailDto.getRemark());
            totalAmount = totalAmount.add(detail.getAmount());
            details.add(detail);
        }

        // 生成处方号
        String prescriptionNo = generatePrescriptionNo();

        // 创建处方
        LocalDateTime now = LocalDateTime.now();
        Prescription prescription = new Prescription();
        prescription.setPrescriptionNo(prescriptionNo);
        prescription.setRecordId(dto.getRecordId());
        prescription.setPatientId(record.getPatientId());
        prescription.setDoctorId(doctorId);
        prescription.setTotalAmount(totalAmount);
        prescription.setStatus(1); // 待发药
        prescription.setRemark(dto.getRemark());
        prescription.setCreatedTime(now);
        prescription.setUpdatedTime(now);
        prescription.setCreatedBy(doctorName);

        prescriptionMapper.insert(prescription);

        // 创建处方明细
        for (PrescriptionDetail detail : details) {
            detail.setPrescriptionId(prescription.getPrescriptionId());
            detail.setCreatedTime(now);
            detail.setUpdatedTime(now);
            prescriptionDetailMapper.insert(detail);
        }

        return getPrescriptionDetail(prescription.getPrescriptionId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrescriptionVo updatePrescription(Long id, PrescriptionUpdateDto dto, Long doctorId) {
        Prescription exist = prescriptionMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("处方不存在");
        }
        if (!exist.getDoctorId().equals(doctorId)) {
            throw new BusinessWarningException("只能修改自己的处方");
        }
        if (exist.getStatus() != 1) {
            throw new BusinessWarningException("处方已发药，无法修改");
        }

        // 删除原明细
        prescriptionDetailMapper.delete(
                new LambdaQueryWrapper<PrescriptionDetail>().eq(PrescriptionDetail::getPrescriptionId, id));

        List<PrescriptionDetail> details = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        for (PrescriptionUpdateDto.PrescriptionDetailDto detailDto : dto.getDetails()) {
            Medicine medicine = requireMedicineForLine(detailDto.getMedicineId(), detailDto.getQuantity(), true);
            PrescriptionDetail detail = toDetailEntity(medicine, detailDto.getQuantity(), detailDto.getDosage(), detailDto.getRemark());
            detail.setPrescriptionId(id);
            detail.setCreatedTime(now);
            detail.setUpdatedTime(now);
            totalAmount = totalAmount.add(detail.getAmount());
            details.add(detail);
        }

        for (PrescriptionDetail detail : details) {
            prescriptionDetailMapper.insert(detail);
        }

        exist.setTotalAmount(totalAmount);
        exist.setRemark(dto.getRemark());
        exist.setUpdatedTime(LocalDateTime.now());
        prescriptionMapper.updateById(exist);

        return getPrescriptionDetail(id);
    }

    @Override
    public PrescriptionVo getPrescriptionDetail(Long id) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new ServiceException("处方不存在");
        }

        // 获取患者信息
        Patient patient = patientMapper.selectById(prescription.getPatientId());
        // 获取医生信息
        Doctor doctor = doctorMapper.selectById(prescription.getDoctorId());

        // 获取处方明细
        List<PrescriptionDetail> details = prescriptionDetailMapper.selectList(
                new LambdaQueryWrapper<PrescriptionDetail>()
                        .eq(PrescriptionDetail::getPrescriptionId, id)
                        .orderByAsc(PrescriptionDetail::getDetailId));

        // 获取药品信息
        List<Long> medicineIds = details.stream()
                .map(PrescriptionDetail::getMedicineId)
                .collect(Collectors.toList());
        Map<Long, Medicine> medicineMap = medicineMapper.selectBatchIds(medicineIds)
                .stream()
                .collect(Collectors.toMap(Medicine::getMedicineId, Function.identity()));

        // 构建 VO
        PrescriptionVo vo = new PrescriptionVo();
        vo.setPrescriptionId(prescription.getPrescriptionId());
        vo.setPrescriptionNo(prescription.getPrescriptionNo());
        vo.setRecordId(prescription.getRecordId());
        vo.setPatientId(prescription.getPatientId());
        vo.setPatientName(patient != null ? patient.getName() : null);
        vo.setDoctorId(prescription.getDoctorId());
        vo.setDoctorName(doctor != null ? doctor.getName() : null);
        vo.setDoctorTitle(doctor != null ? doctor.getTitle() : null);
        vo.setTotalAmount(prescription.getTotalAmount());
        vo.setStatus(prescription.getStatus());
        vo.setStatusText(getStatusText(prescription.getStatus()));
        vo.setRemark(prescription.getRemark());
        vo.setCreatedTime(prescription.getCreatedTime());
        vo.setUpdatedTime(prescription.getUpdatedTime());

        List<PrescriptionDetailVo> detailVos = new ArrayList<>();
        for (PrescriptionDetail detail : details) {
            Medicine medicine = medicineMap.get(detail.getMedicineId());
            PrescriptionDetailVo detailVo = new PrescriptionDetailVo();
            detailVo.setDetailId(detail.getDetailId());
            detailVo.setMedicineId(detail.getMedicineId());
            detailVo.setMedicineName(medicine != null ? medicine.getName() : null);
            detailVo.setMedicineCode(medicine != null ? medicine.getMedicineCode() : null);
            detailVo.setSpec(medicine != null ? medicine.getSpec() : null);
            detailVo.setUnit(medicine != null ? medicine.getUnit() : null);
            detailVo.setQuantity(detail.getQuantity());
            detailVo.setDosage(detail.getDosage());
            detailVo.setUnitPrice(detail.getUnitPrice());
            detailVo.setAmount(detail.getAmount());
            detailVo.setRemark(detail.getRemark());
            detailVo.setCreatedTime(detail.getCreatedTime());
            detailVos.add(detailVo);
        }
        vo.setDetails(detailVos);

        return vo;
    }

    @Override
    public List<PrescriptionVo> getPrescriptionsByRecordId(Long recordId) {
        List<Prescription> prescriptions = prescriptionMapper.selectList(
                new LambdaQueryWrapper<Prescription>()
                        .eq(Prescription::getRecordId, recordId)
                        .orderByDesc(Prescription::getCreatedTime));
        return prescriptions.stream()
                .map(p -> getPrescriptionDetail(p.getPrescriptionId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PrescriptionVo> getPrescriptionsByPatientId(Long patientId) {
        List<Prescription> prescriptions = prescriptionMapper.selectList(
                new LambdaQueryWrapper<Prescription>()
                        .eq(Prescription::getPatientId, patientId)
                        .orderByDesc(Prescription::getCreatedTime));
        return prescriptions.stream()
                .map(p -> getPrescriptionDetail(p.getPrescriptionId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePrescription(Long id, Long doctorId) {
        Prescription exist = prescriptionMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("处方不存在");
        }
        if (!exist.getDoctorId().equals(doctorId)) {
            throw new BusinessWarningException("只能删除自己的处方");
        }
        if (exist.getStatus() != 1) {
            throw new BusinessWarningException("处方已发药，无法删除");
        }

        prescriptionDetailMapper.delete(
                new LambdaQueryWrapper<PrescriptionDetail>().eq(PrescriptionDetail::getPrescriptionId, id));
        prescriptionMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispensePrescription(Long id, Long nurseId) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null) {
            throw new ServiceException("处方不存在");
        }
        if (prescription.getStatus() != 1) {
            throw new BusinessWarningException("处方状态不是待发药");
        }

        // 检查库存并扣减
        List<PrescriptionDetail> details = prescriptionDetailMapper.selectList(
                new LambdaQueryWrapper<PrescriptionDetail>().eq(PrescriptionDetail::getPrescriptionId, id));

        for (PrescriptionDetail detail : details) {
            Medicine medicine = medicineMapper.selectById(detail.getMedicineId());
            if (medicine == null) {
                throw new BusinessWarningException("药品不存在");
            }
            if (medicine.getStockQuantity() < detail.getQuantity()) {
                throw new BusinessWarningException("药品库存不足: " + medicine.getName() +
                        "，当前库存: " + medicine.getStockQuantity());
            }
            medicine.setStockQuantity(medicine.getStockQuantity() - detail.getQuantity());
            medicine.setUpdatedTime(LocalDateTime.now());
            medicineMapper.updateById(medicine);
        }

        prescription.setStatus(2); // 已发药
        prescription.setUpdatedTime(LocalDateTime.now());
        prescriptionMapper.updateById(prescription);
    }

    @Override
    public List<PrescriptionVo> getPendingDispenseList(String keyword, Integer status) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Prescription::getStatus, status);
        } else {
            wrapper.eq(Prescription::getStatus, 1);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Prescription::getPrescriptionNo, keyword));
        }
        wrapper.orderByDesc(Prescription::getCreatedTime);

        List<Prescription> prescriptions = prescriptionMapper.selectList(wrapper);
        return prescriptions.stream()
                .map(p -> getPrescriptionDetail(p.getPrescriptionId()))
                .collect(Collectors.toList());
    }

    private Medicine requireMedicineForLine(Long medicineId, int quantity, boolean checkStock) {
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new BusinessWarningException("药品不存在");
        }
        if (medicine.getStatus() == null || medicine.getStatus() != 1) {
            throw new BusinessWarningException("药品已停用: " + medicine.getName());
        }
        if (checkStock && medicine.getStockQuantity() < quantity) {
            throw new BusinessWarningException("药品库存不足: " + medicine.getName() +
                    "，当前库存: " + medicine.getStockQuantity());
        }
        return medicine;
    }

    private static PrescriptionDetail toDetailEntity(Medicine medicine, int quantity, String dosage, String remark) {
        BigDecimal amount = medicine.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
        PrescriptionDetail detail = new PrescriptionDetail();
        detail.setMedicineId(medicine.getMedicineId());
        detail.setQuantity(quantity);
        detail.setDosage(dosage);
        detail.setUnitPrice(medicine.getUnitPrice());
        detail.setAmount(amount);
        detail.setRemark(remark);
        return detail;
    }

    private String generatePrescriptionNo() {
        return "PRE" + DateUtil.format(LocalDateTime.now(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(4);
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "待发药";
            case 2: return "已发药";
            case 3: return "已取消";
            default: return "未知";
        }
    }

    @Override
    public List<PrescriptionVo> getPrescriptionsByDoctorId(Long doctorId, Integer status) {
        LambdaQueryWrapper<Prescription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Prescription::getDoctorId, doctorId);
        if (status != null) {
            wrapper.eq(Prescription::getStatus, status);
        }
        wrapper.orderByDesc(Prescription::getCreatedTime);

        List<Prescription> prescriptions = prescriptionMapper.selectList(wrapper);
        return prescriptions.stream()
                .map(p -> getPrescriptionDetail(p.getPrescriptionId()))
                .collect(Collectors.toList());
    }

}