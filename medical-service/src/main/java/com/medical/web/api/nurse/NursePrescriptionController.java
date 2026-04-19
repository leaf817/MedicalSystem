package com.medical.web.api.nurse;

import com.medical.common.response.ResultVo;
import com.medical.domain.vo.PrescriptionVo;
import com.medical.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "药房端-处方管理")
@RestController
@RequestMapping("/api/nurse/prescription")
@RequiredArgsConstructor
public class NursePrescriptionController {

    private final PrescriptionService prescriptionService;

    @Operation(summary = "待发药列表")
    @GetMapping("/pending")
    public ResultVo<List<PrescriptionVo>> pendingList(
            @RequestParam(required = false) String keyword) {
        List<PrescriptionVo> list = prescriptionService.getPendingDispenseList(keyword, 1);
        return ResultVo.ok(list);
    }

    @Operation(summary = "已发药列表")
    @GetMapping("/dispensed")
    public ResultVo<List<PrescriptionVo>> dispensedList(
            @RequestParam(required = false) String keyword) {
        List<PrescriptionVo> list = prescriptionService.getPendingDispenseList(keyword, 2);
        return ResultVo.ok(list);
    }

    @Operation(summary = "发药确认")
    @PutMapping("/{id}/dispense")
    public ResultVo<Void> dispense(@PathVariable Long id) {
        // TODO: 实际实现中需要获取当前登录护士ID
        prescriptionService.dispensePrescription(id, 1L);
        return ResultVo.ok();
    }

    @Operation(summary = "处方详情")
    @GetMapping("/{id}")
    public ResultVo<PrescriptionVo> detail(@PathVariable Long id) {
        PrescriptionVo vo = prescriptionService.getPrescriptionDetail(id);
        return ResultVo.ok(vo);
    }
}