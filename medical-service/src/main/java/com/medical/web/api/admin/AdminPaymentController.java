package com.medical.web.api.admin;

import com.medical.common.pagination.PageResult;
import com.medical.common.response.ResultVo;
import com.medical.domain.vo.PaymentVo;
import com.medical.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 管理端 — 收费流水查询（复用统一 PaymentService）
 */
@RestController
@RequestMapping("/api/admin/payment")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

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
}
