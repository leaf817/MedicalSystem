package com.medical.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.pagination.PageResult;
import com.medical.domain.entity.Appointment;
import com.medical.domain.entity.Patient;
import com.medical.domain.entity.Payment;
import com.medical.domain.entity.Prescription;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.PaymentVo;
import com.medical.mapper.AppointmentMapper;
import com.medical.mapper.PatientMapper;
import com.medical.mapper.PaymentMapper;
import com.medical.mapper.PrescriptionMapper;
import com.medical.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final AppointmentMapper appointmentMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final PatientMapper patientMapper;
    private final SysUserMapper sysUserMapper;

    @Autowired
    @Lazy
    private AppointmentService appointmentService;

    @Transactional(rollbackFor = Exception.class)
    public Payment charge(String bizType, Long bizId, String payMethod, BigDecimal amountOverride,
                          Long operatorId, String remark) {
        String type = bizType != null ? bizType.trim().toUpperCase() : "";
        return switch (type) {
            case "APPOINTMENT" -> chargeAppointment(bizId, payMethod, amountOverride, operatorId, remark);
            case "PRESCRIPTION" -> chargePrescription(bizId, payMethod, amountOverride, operatorId, remark);
            default -> throw new BusinessWarningException("不支持的业务类型：" + bizType);
        };
    }

    private Payment chargeAppointment(Long appointmentId, String payMethod, BigDecimal amountOverride,
                                      Long operatorId, String remark) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessWarningException("预约记录不存在");
        }
        if (appointment.getStatus() != 1) {
            throw new BusinessWarningException("只有待就诊状态的预约可以收费");
        }
        if (appointment.getPaid() != null && appointment.getPaid() == 1) {
            throw new BusinessWarningException("该预约已支付");
        }
        long existPaid = paymentMapper.selectCount(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getBizType, "APPOINTMENT")
                .eq(Payment::getBizId, appointmentId)
                .eq(Payment::getStatus, 1));
        if (existPaid > 0) {
            throw new BusinessWarningException("该预约已有有效支付记录");
        }

        BigDecimal amount = amountOverride != null ? amountOverride : appointment.getFeeAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessWarningException("收费金额无效");
        }

        LocalDateTime now = LocalDateTime.now();
        Payment payment = buildPayment(appointment.getPatientId(), "APPOINTMENT", appointmentId,
                amount, payMethod, operatorId, remark, now);
        paymentMapper.insert(payment);

        appointment.setPaid(1);
        appointment.setPaidTime(now);
        appointment.setUpdatedTime(now);
        appointmentMapper.updateById(appointment);
        appointmentService.reassignQueueAfterPayment(appointment.getDoctorId(), appointment.getAppointmentDate());

        return payment;
    }

    private Payment chargePrescription(Long prescriptionId, String payMethod, BigDecimal amountOverride,
                                       Long operatorId, String remark) {
        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            throw new BusinessWarningException("处方不存在");
        }
        if (prescription.getStatus() != null && prescription.getStatus() == 3) {
            throw new BusinessWarningException("已取消的处方无法收费");
        }
        long existPaid = paymentMapper.selectCount(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getBizType, "PRESCRIPTION")
                .eq(Payment::getBizId, prescriptionId)
                .eq(Payment::getStatus, 1));
        if (existPaid > 0) {
            throw new BusinessWarningException("该处方已缴费");
        }

        BigDecimal amount = amountOverride != null ? amountOverride : prescription.getTotalAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessWarningException("收费金额无效");
        }

        LocalDateTime now = LocalDateTime.now();
        Payment payment = buildPayment(prescription.getPatientId(), "PRESCRIPTION", prescriptionId,
                amount, payMethod, operatorId, remark, now);
        paymentMapper.insert(payment);
        return payment;
    }

    @Transactional(rollbackFor = Exception.class)
    public Payment refund(Long paymentId, Long operatorId, String reason) {
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new BusinessWarningException("支付记录不存在");
        }
        if (payment.getStatus() == null || payment.getStatus() != 1) {
            throw new BusinessWarningException("仅已支付记录可退费");
        }

        String bizType = payment.getBizType();
        if ("APPOINTMENT".equals(bizType)) {
            refundAppointment(payment);
        } else if ("PRESCRIPTION".equals(bizType)) {
            refundPrescription(payment);
        } else {
            throw new BusinessWarningException("该业务类型暂不支持退费");
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setStatus(2);
        payment.setRemark(mergeRemark(payment.getRemark(), reason));
        payment.setUpdatedTime(now);
        payment.setUpdatedBy(operatorId != null ? String.valueOf(operatorId) : null);
        paymentMapper.updateById(payment);
        return payment;
    }

    private void refundAppointment(Payment payment) {
        Appointment appointment = appointmentMapper.selectById(payment.getBizId());
        if (appointment == null) {
            throw new BusinessWarningException("关联预约不存在");
        }
        if (appointment.getStatus() != 1) {
            throw new BusinessWarningException("仅待就诊且未接诊的预约可退挂号费");
        }
        appointment.setPaid(0);
        appointment.setPaidTime(null);
        appointment.setUpdatedTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);
        appointmentService.reassignQueueAfterPayment(appointment.getDoctorId(), appointment.getAppointmentDate());
    }

    private void refundPrescription(Payment payment) {
        Prescription prescription = prescriptionMapper.selectById(payment.getBizId());
        if (prescription == null) {
            throw new BusinessWarningException("关联处方不存在");
        }
        if (prescription.getStatus() != null && prescription.getStatus() >= 2) {
            throw new BusinessWarningException("已发药或已完成的处方不可退费");
        }
    }

    public PageResult<PaymentVo> pageQuery(Long current, Long size, String keyword, String bizType,
                                           Integer status, Long patientId,
                                           LocalDate dateFrom, LocalDate dateTo) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(bizType)) {
            wrapper.eq(Payment::getBizType, bizType.trim().toUpperCase());
        }
        if (status != null) {
            wrapper.eq(Payment::getStatus, status);
        }
        if (patientId != null) {
            wrapper.eq(Payment::getPatientId, patientId);
        }
        if (dateFrom != null) {
            wrapper.ge(Payment::getPayTime, dateFrom.atStartOfDay());
        }
        if (dateTo != null) {
            wrapper.lt(Payment::getPayTime, dateTo.plusDays(1).atStartOfDay());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Payment::getPaymentNo, kw).or().like(Payment::getRemark, kw));
        }
        wrapper.orderByDesc(Payment::getPayTime).orderByDesc(Payment::getPaymentId);

        Page<Payment> page = paymentMapper.selectPage(new Page<>(current, size), wrapper);
        List<PaymentVo> list = toVoList(page.getRecords());

        PageResult<PaymentVo> result = new PageResult<>();
        result.setCurrentPage(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setList(list);
        return result;
    }

    public PaymentVo getDetail(Long paymentId) {
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new BusinessWarningException("支付记录不存在");
        }
        List<PaymentVo> vos = toVoList(List.of(payment));
        return vos.isEmpty() ? null : vos.get(0);
    }

    public BigDecimal sumTodayPaidAmount() {
        LocalDate today = LocalDate.now();
        List<Payment> list = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getStatus, 1)
                .ge(Payment::getPayTime, today.atStartOfDay())
                .lt(Payment::getPayTime, today.plusDays(1).atStartOfDay()));
        return list.stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long countTodayRefunds() {
        LocalDate today = LocalDate.now();
        return paymentMapper.selectCount(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getStatus, 2)
                .ge(Payment::getUpdatedTime, today.atStartOfDay())
                .lt(Payment::getUpdatedTime, today.plusDays(1).atStartOfDay()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordPatientSelfPay(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null || !appointment.getPatientId().equals(patientId)) {
            return;
        }
        long exist = paymentMapper.selectCount(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getBizType, "APPOINTMENT")
                .eq(Payment::getBizId, appointmentId)
                .eq(Payment::getStatus, 1));
        if (exist > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Payment payment = buildPayment(patientId, "APPOINTMENT", appointmentId,
                appointment.getFeeAmount(), "ONLINE", null, "患者自助支付", now);
        paymentMapper.insert(payment);
    }

    private Payment buildPayment(Long patientId, String bizType, Long bizId, BigDecimal amount,
                                 String payMethod, Long operatorId, String remark, LocalDateTime now) {
        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setPatientId(patientId);
        payment.setBizType(bizType);
        payment.setBizId(bizId);
        payment.setAmount(amount);
        payment.setPayMethod(normalizePayMethod(payMethod));
        payment.setPayTime(now);
        payment.setOperatorId(operatorId);
        payment.setStatus(1);
        payment.setRemark(remark);
        payment.setCreatedTime(now);
        payment.setUpdatedTime(now);
        if (operatorId != null) {
            payment.setCreatedBy(String.valueOf(operatorId));
            payment.setUpdatedBy(String.valueOf(operatorId));
        }
        return payment;
    }

    private String generatePaymentNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 9000) + 1000;
        return "PAY" + timestamp + random;
    }

    private String normalizePayMethod(String payMethod) {
        if (!StringUtils.hasText(payMethod)) {
            return "CASH";
        }
        return payMethod.trim().toUpperCase();
    }

    private String mergeRemark(String existing, String reason) {
        if (!StringUtils.hasText(reason)) {
            return existing;
        }
        String tag = "退费原因：" + reason.trim();
        if (!StringUtils.hasText(existing)) {
            return tag;
        }
        return existing + "；" + tag;
    }

    private List<PaymentVo> toVoList(List<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return List.of();
        }
        Set<Long> patientIds = payments.stream().map(Payment::getPatientId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Patient> patientMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            for (Patient p : patientMapper.selectBatchIds(patientIds)) {
                if (p != null) {
                    patientMap.put(p.getPatientId(), p);
                }
            }
        }
        Set<Long> operatorIds = payments.stream().map(Payment::getOperatorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> operatorNameMap = new HashMap<>();
        if (!operatorIds.isEmpty()) {
            for (SysUser u : sysUserMapper.selectBatchIds(operatorIds)) {
                if (u != null) {
                    operatorNameMap.put(u.getUserId(), u.getName());
                }
            }
        }

        Map<Long, String> appointmentNoMap = loadAppointmentNos(payments);
        Map<Long, String> prescriptionNoMap = loadPrescriptionNos(payments);

        return payments.stream().map(p -> {
            PaymentVo vo = new PaymentVo();
            vo.setPaymentId(p.getPaymentId());
            vo.setPaymentNo(p.getPaymentNo());
            vo.setPatientId(p.getPatientId());
            Patient patient = patientMap.get(p.getPatientId());
            if (patient != null) {
                vo.setPatientName(patient.getName());
            }
            vo.setBizType(p.getBizType());
            vo.setBizTypeText(bizTypeText(p.getBizType()));
            vo.setBizId(p.getBizId());
            if ("APPOINTMENT".equals(p.getBizType())) {
                vo.setBizNo(appointmentNoMap.get(p.getBizId()));
            } else if ("PRESCRIPTION".equals(p.getBizType())) {
                vo.setBizNo(prescriptionNoMap.get(p.getBizId()));
            }
            vo.setAmount(p.getAmount());
            vo.setPayMethod(p.getPayMethod());
            vo.setPayMethodText(payMethodText(p.getPayMethod()));
            vo.setPayTime(p.getPayTime());
            vo.setOperatorId(p.getOperatorId());
            vo.setOperatorName(operatorNameMap.get(p.getOperatorId()));
            vo.setStatus(p.getStatus());
            vo.setStatusText(p.getStatus() != null && p.getStatus() == 2 ? "已退款" : "已支付");
            vo.setRemark(p.getRemark());
            vo.setCreatedTime(p.getCreatedTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, String> loadAppointmentNos(List<Payment> payments) {
        List<Long> ids = payments.stream()
                .filter(p -> "APPOINTMENT".equals(p.getBizType()))
                .map(Payment::getBizId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> map = new HashMap<>();
        for (Appointment a : appointmentMapper.selectBatchIds(ids)) {
            if (a != null) {
                map.put(a.getAppointmentId(), a.getAppointmentNo());
            }
        }
        return map;
    }

    private Map<Long, String> loadPrescriptionNos(List<Payment> payments) {
        List<Long> ids = payments.stream()
                .filter(p -> "PRESCRIPTION".equals(p.getBizType()))
                .map(Payment::getBizId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> map = new HashMap<>();
        for (Prescription pr : prescriptionMapper.selectBatchIds(ids)) {
            if (pr != null) {
                map.put(pr.getPrescriptionId(), pr.getPrescriptionNo());
            }
        }
        return map;
    }

    private String bizTypeText(String bizType) {
        if (bizType == null) return "未知";
        return switch (bizType) {
            case "APPOINTMENT" -> "挂号费";
            case "PRESCRIPTION" -> "处方费";
            case "EXAM" -> "检查费";
            default -> bizType;
        };
    }

    private String payMethodText(String payMethod) {
        if (payMethod == null) return "未知";
        return switch (payMethod) {
            case "CASH" -> "现金";
            case "WECHAT" -> "微信";
            case "ALIPAY" -> "支付宝";
            case "CARD" -> "银行卡";
            case "ONLINE" -> "在线支付";
            default -> payMethod;
        };
    }
}
