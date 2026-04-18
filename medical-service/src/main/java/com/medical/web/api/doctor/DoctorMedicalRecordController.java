// medical-service/src/main/java/com/medical/web/api/doctor/DoctorMedicalRecordController.java
package com.medical.web.api.doctor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.response.ResultVo;
import com.medical.domain.dto.MedicalRecordSaveDto;
import com.medical.domain.entity.Doctor;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.MedicalRecordVo;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.SysUserMapper;
import com.medical.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor/medical-record")
@RequiredArgsConstructor
public class DoctorMedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final DoctorMapper doctorMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 获取所有病历（当前医生）
     * GET /api/doctor/medical-record/all
     */
    @GetMapping("/all")
    public ResultVo<List<MedicalRecordVo>> getAllRecords() {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return ResultVo.ok(null);
        }
        List<MedicalRecordVo> records = medicalRecordService.getDoctorRecords(doctorId);
        return ResultVo.ok(records);
    }

    /**
     * 获取患者历史病历
     * GET /api/doctor/medical-record/patient/{patientId}
     */
    @GetMapping("/patient/{patientId}")
    public ResultVo<List<MedicalRecordVo>> getPatientHistory(@PathVariable Long patientId) {
        List<MedicalRecordVo> records = medicalRecordService.getPatientHistory(patientId);
        return ResultVo.ok(records);
    }

    /**
     * 获取病历详情
     * GET /api/doctor/medical-record/{recordId}
     */
    @GetMapping("/{recordId}")
    public ResultVo<MedicalRecordVo> getRecordDetail(@PathVariable Long recordId) {
        MedicalRecordVo record = medicalRecordService.getRecordDetail(recordId);
        return ResultVo.ok(record);
    }

    /**
     * 保存病历
     * POST /api/doctor/medical-record/save
     */
    @PostMapping("/save")
    public ResultVo<MedicalRecordVo> saveRecord(@RequestBody MedicalRecordSaveDto dto) {
        Long doctorId = getCurrentDoctorId();
        String doctorName = getCurrentDoctorName();
        if (doctorId == null) {
            return ResultVo.ok(null);
        }
        MedicalRecordVo record = medicalRecordService.saveRecord(dto, doctorId, doctorName);
        return ResultVo.ok(record);
    }

    /**
     * 删除病历
     * DELETE /api/doctor/medical-record/{recordId}
     */
    @DeleteMapping("/{recordId}")
    public ResultVo<Void> deleteRecord(@PathVariable Long recordId) {
        Long doctorId = getCurrentDoctorId();
        if (doctorId == null) {
            return ResultVo.ok(null);
        }
        boolean result = medicalRecordService.deleteRecord(recordId, doctorId);
        if (result) {
            return ResultVo.ok();
        } else {
            return ResultVo.bizWarning("删除失败");
        }
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        String username = getCurrentUsername();
        if (username == null) {
            return null;
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = sysUserMapper.selectOne(wrapper);
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前医生ID
     */
    private Long getCurrentDoctorId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Doctor::getUserId, userId);
        Doctor doctor = doctorMapper.selectOne(wrapper);
        return doctor != null ? doctor.getDoctorId() : null;
    }

    /**
     * 获取当前医生姓名
     */
    private String getCurrentDoctorName() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Doctor::getUserId, userId);
        Doctor doctor = doctorMapper.selectOne(wrapper);
        return doctor != null ? doctor.getName() : null;
    }

    /**
     * 获取当前登录用户名
     */
    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        return null;
    }
}