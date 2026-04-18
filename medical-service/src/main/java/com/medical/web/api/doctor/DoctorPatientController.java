// medical-service/src/main/java/com/medical/web/api/doctor/DoctorPatientController.java
package com.medical.web.api.doctor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.medical.common.response.ResultVo;
import com.medical.domain.entity.Patient;
import com.medical.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctor/patient")
@RequiredArgsConstructor
public class DoctorPatientController {

    private final PatientMapper patientMapper;

    /**
     * 获取所有患者
     * GET /api/doctor/patient/all
     */
    @GetMapping("/all")
    public ResultVo<List<Patient>> getAllPatients() {
        LambdaQueryWrapper<Patient> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Patient::getStatus, 1)
                .orderByDesc(Patient::getCreatedTime)
                .last("LIMIT 100");
        List<Patient> patients = patientMapper.selectList(wrapper);
        return ResultVo.ok(patients);
    }

    /**
     * 搜索患者
     * GET /api/doctor/patient/search?keyword=xxx
     */
    @GetMapping("/search")
    public ResultVo<List<Patient>> searchPatients(@RequestParam String keyword) {
        LambdaQueryWrapper<Patient> wrapper = Wrappers.lambdaQuery();
        wrapper.like(Patient::getName, keyword)
                .or()
                .like(Patient::getPatientNo, keyword)
                .or()
                .like(Patient::getPhone, keyword)
                .eq(Patient::getStatus, 1)
                .orderByDesc(Patient::getCreatedTime)
                .last("LIMIT 20");
        List<Patient> patients = patientMapper.selectList(wrapper);
        return ResultVo.ok(patients);
    }
}