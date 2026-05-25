package com.medical.web.api.reception;

import com.medical.common.pagination.PageResult;
import com.medical.common.response.ResultVo;
import com.medical.domain.dto.PaymentChargeDto;
import com.medical.domain.dto.PaymentRefundDto;
import com.medical.domain.entity.Payment;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.PaymentVo;
import com.medical.mapper.SysUserMapper;
import com.medical.domain.vo.PrescriptionVo;
import com.medical.service.PaymentService;
import com.medical.service.PrescriptionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reception/payment")
@RequiredArgsConstructor
public class ReceptionPaymentController {

    private final PaymentService paymentService;
    private final PrescriptionService prescriptionService;
    private final SysUserMapper sysUserMapper;

    @GetMapping("/unpaid-prescriptions")
    public ResultVo<java.util.List<PrescriptionVo>> unpaidPrescriptions(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultVo.ok(prescriptionService.listPendingUnpaid(keyword));
    }

    @PostMapping("/charge")
    public ResultVo<Map<String, Object>> charge(@Valid @RequestBody PaymentChargeDto dto) {
        Long operatorId = getCurrentUserId();
        Payment payment = paymentService.createPayment(
                dto.getBizType(),
                dto.getBizId(),
                dto.getAmount(),
                dto.getPayMethod(),
                operatorId,
                dto.getRemark());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentId", payment.getPaymentId());
        data.put("paymentNo", payment.getPaymentNo());
        data.put("amount", payment.getAmount());
        return ResultVo.ok(data);
    }

    @PostMapping("/refund")
    public ResultVo<Map<String, Object>> refund(@Valid @RequestBody PaymentRefundDto dto) {
        Long operatorId = getCurrentUserId();
        Payment payment = paymentService.refundPayment(dto.getPaymentId(), operatorId, dto.getReason());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("paymentId", payment.getPaymentId());
        data.put("paymentNo", payment.getPaymentNo());
        data.put("status", payment.getStatus());
        return ResultVo.ok(data);
    }

    @GetMapping("/page")
    public ResultVo<PageResult<PaymentVo>> page(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "bizType", required = false) String bizType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTo) {
        return ResultVo.ok(paymentService.pageQuery(current, size, keyword, bizType, status, patientId, dateFrom, dateTo));
    }

    @GetMapping("/{paymentId}")
    public ResultVo<PaymentVo> detail(@PathVariable Long paymentId) {
        return ResultVo.ok(paymentService.getDetail(paymentId));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, auth.getName()));
        return user != null ? user.getUserId() : null;
    }
}
