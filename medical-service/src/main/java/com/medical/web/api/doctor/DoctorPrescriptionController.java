package com.medical.web.api.doctor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.response.ResultVo;
import com.medical.domain.dto.PrescriptionCreateDto;
import com.medical.domain.dto.PrescriptionUpdateDto;
import com.medical.domain.entity.Doctor;
import com.medical.domain.entity.Medicine;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.MedicineListVo;
import com.medical.domain.vo.PrescriptionVo;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.MedicineMapper;
import com.medical.mapper.SysUserMapper;
import com.medical.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "医生端-处方管理")
@RestController
@RequestMapping("/api/doctor/prescription")
@RequiredArgsConstructor
public class DoctorPrescriptionController {

    private final PrescriptionService prescriptionService;
    private final MedicineMapper medicineMapper;
    private final SysUserMapper sysUserMapper;
    private final DoctorMapper doctorMapper;

    private Long getCurrentDoctorId(Authentication authentication) {
        String username = authentication.getName();
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new BusinessWarningException("用户不存在");
        }
        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>().eq(Doctor::getUserId, user.getUserId())
        );
        if (doctor == null) {
            throw new BusinessWarningException("医生档案不存在");
        }
        return doctor.getDoctorId();
    }

    private String getCurrentDoctorName(Authentication authentication) {
        String username = authentication.getName();
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        return user != null ? user.getName() : "未知医生";
    }

    @Operation(summary = "开立处方")
    @PostMapping
    public ResultVo<PrescriptionVo> create(
            @Valid @RequestBody PrescriptionCreateDto dto,
            Authentication authentication) {
        Long doctorId = getCurrentDoctorId(authentication);
        String doctorName = getCurrentDoctorName(authentication);
        PrescriptionVo vo = prescriptionService.createPrescription(dto, doctorId, doctorName);
        return ResultVo.ok(vo);
    }

    @Operation(summary = "更新处方")
    @PutMapping("/{id}")
    public ResultVo<PrescriptionVo> update(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionUpdateDto dto,
            Authentication authentication) {
        Long doctorId = getCurrentDoctorId(authentication);
        PrescriptionVo vo = prescriptionService.updatePrescription(id, dto, doctorId);
        return ResultVo.ok(vo);
    }

    @Operation(summary = "删除处方")
    @DeleteMapping("/{id}")
    public ResultVo<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {
        Long doctorId = getCurrentDoctorId(authentication);
        prescriptionService.deletePrescription(id, doctorId);
        return ResultVo.ok();
    }

    @Operation(summary = "获取处方详情")
    @GetMapping("/{id}")
    public ResultVo<PrescriptionVo> detail(@PathVariable Long id) {
        PrescriptionVo vo = prescriptionService.getPrescriptionDetail(id);
        return ResultVo.ok(vo);
    }

    @Operation(summary = "获取病历下的处方列表")
    @GetMapping("/record/{recordId}")
    public ResultVo<List<PrescriptionVo>> listByRecord(@PathVariable Long recordId) {
        List<PrescriptionVo> list = prescriptionService.getPrescriptionsByRecordId(recordId);
        return ResultVo.ok(list);
    }

    @Operation(summary = "获取患者的处方列表")
    @GetMapping("/patient/{patientId}")
    public ResultVo<List<PrescriptionVo>> listByPatient(@PathVariable Long patientId) {
        List<PrescriptionVo> list = prescriptionService.getPrescriptionsByPatientId(patientId);
        return ResultVo.ok(list);
    }

    @Operation(summary = "搜索药品")
    @GetMapping("/medicines/search")
    public ResultVo<List<MedicineListVo>> searchMedicines(
            @RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResultVo.ok(List.of());
        }

        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Medicine::getStatus, 1)
                .and(w -> w.like(Medicine::getName, keyword.trim())
                        .or().like(Medicine::getCommonName, keyword.trim())
                        .or().like(Medicine::getMedicineCode, keyword.trim()))
                .last("LIMIT 20");

        List<Medicine> list = medicineMapper.selectList(wrapper);

        List<MedicineListVo> voList = list.stream().map(m -> {
            MedicineListVo vo = new MedicineListVo();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).collect(Collectors.toList());

        return ResultVo.ok(voList);
    }

    @Operation(summary = "获取当前医生的处方列表")
    @GetMapping("/my")
    public ResultVo<List<PrescriptionVo>> getMyPrescriptions(
            @RequestParam(required = false) Integer status,
            Authentication authentication) {
        Long doctorId = getCurrentDoctorId(authentication);
        List<PrescriptionVo> list = prescriptionService.getPrescriptionsByDoctorId(doctorId, status);
        return ResultVo.ok(list);
    }
}