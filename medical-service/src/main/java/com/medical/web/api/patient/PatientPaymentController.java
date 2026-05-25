package com.medical.web.api.patient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.response.ResultVo;
import com.medical.domain.entity.Patient;
import com.medical.domain.entity.Prescription;
import com.medical.domain.entity.SysUser;
import com.medical.mapper.PatientMapper;
import com.medical.mapper.PrescriptionMapper;
import com.medical.mapper.SysUserMapper;
import com.medical.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 患者端 — 统一支付（挂号费、处方费等）
 */
@RestController
@RequestMapping("/api/patient/payment")
@RequiredArgsConstructor
public class PatientPaymentController {

    private final PaymentService paymentService;
    private final PatientMapper patientMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final SysUserMapper sysUserMapper;

    @PutMapping("/prescription/{prescriptionId}")
    public ResultVo<Map<String, Object>> payPrescription(@PathVariable Long prescriptionId) {
        Long patientId = getCurrentPatientId();
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessWarningException("处方不存在");
        }
        if (!prescription.getPatientId().equals(patientId)) {
            throw new BusinessWarningException("无权操作此处方");
        }
        var payment = paymentService.createPayment(
                "PRESCRIPTION",
                prescriptionId,
                prescription.getTotalAmount(),
                "ONLINE",
                null,
                "患者自助支付");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentId", payment.getPaymentId());
        data.put("paymentNo", payment.getPaymentNo());
        return ResultVo.ok(data);
    }

    private Long getCurrentPatientId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessWarningException("请先登录");
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, auth.getName()));
        if (user == null) {
            throw new BusinessWarningException("用户不存在");
        }
        Patient patient = patientMapper.selectOne(
                new LambdaQueryWrapper<Patient>().eq(Patient::getUserId, user.getUserId()));
        if (patient == null) {
            throw new BusinessWarningException("患者档案不存在");
        }
        return patient.getPatientId();
    }
}
