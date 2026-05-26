package com.medical.web.api.admin;

import com.medical.common.pagination.PageResult;
import com.medical.common.response.ResultVo;
import com.medical.domain.vo.PaymentVo;
import com.medical.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 管理端 — 收费流水查询（复用统一 PaymentService）
 */
@Tag(name = "管理端-收费查询")
@RestController
@RequestMapping("/api/admin/payment")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "收费流水分页查询")
    @GetMapping("/page")
    public ResultVo<PageResult<PaymentVo>> page(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "bizType", required = false) String bizType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "payMethod", required = false) String payMethod,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTo) {
        return ResultVo.ok(paymentService.pageQuery(current, size, keyword, bizType, status, patientId, payMethod, dateFrom, dateTo));
    }

    @Operation(summary = "导出收费流水 CSV")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "bizType", required = false) String bizType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "payMethod", required = false) String payMethod,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dateTo) {
        byte[] body = paymentService.exportCsv(keyword, bizType, status, patientId, payMethod, dateFrom, dateTo);
        String filename = "payment_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @Operation(summary = "收费流水详情")
    @GetMapping("/{paymentId}")
    public ResultVo<PaymentVo> detail(@PathVariable Long paymentId) {
        return ResultVo.ok(paymentService.getDetail(paymentId));
    }
}
