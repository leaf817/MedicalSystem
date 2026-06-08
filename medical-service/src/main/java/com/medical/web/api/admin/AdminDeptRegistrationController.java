package com.medical.web.api.admin;

import com.medical.common.response.ResultVo;
import com.medical.domain.dto.DeptAlertRuleSaveDto;
import com.medical.domain.vo.DeptAlertRuleVo;
import com.medical.domain.vo.DeptEpidemicHintVo;
import com.medical.domain.vo.DeptRegistrationAlertVo;
import com.medical.domain.vo.DeptRegistrationBoardVo;
import com.medical.domain.vo.DeptRegistrationTrendPointVo;
import com.medical.service.DeptRegistrationBoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "管理端-科室挂号看板")
@RestController
@RequestMapping("/api/admin/dept/registration")
@RequiredArgsConstructor
public class AdminDeptRegistrationController {

    private final DeptRegistrationBoardService deptRegistrationBoardService;

    @Operation(summary = "科室挂号看板汇总")
    @GetMapping("/board")
    public ResultVo<DeptRegistrationBoardVo> board(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to) {
        return ResultVo.ok(deptRegistrationBoardService.getBoard(from, to));
    }

    @Operation(summary = "科室挂号趋势（含未来预约）")
    @GetMapping("/trend")
    public ResultVo<List<DeptRegistrationTrendPointVo>> trend(
            @RequestParam Long deptId,
            @RequestParam(defaultValue = "14") Integer days,
            @RequestParam(defaultValue = "2") Integer futureDays) {
        return ResultVo.ok(deptRegistrationBoardService.getTrend(deptId, days, futureDays));
    }

    @Operation(summary = "当前预警列表")
    @GetMapping("/alerts")
    public ResultVo<List<DeptRegistrationAlertVo>> alerts() {
        return ResultVo.ok(deptRegistrationBoardService.evaluateAlerts());
    }

    @Operation(summary = "疾病趋势提示")
    @GetMapping("/epidemic-hints")
    public ResultVo<List<DeptEpidemicHintVo>> epidemicHints() {
        return ResultVo.ok(deptRegistrationBoardService.buildEpidemicHints());
    }

    @Operation(summary = "预警规则列表")
    @GetMapping("/alert-rules")
    public ResultVo<List<DeptAlertRuleVo>> alertRules() {
        return ResultVo.ok(deptRegistrationBoardService.listAlertRules());
    }

    @Operation(summary = "新增预警规则")
    @PostMapping("/alert-rules")
    public ResultVo<Void> createAlertRule(@Valid @RequestBody DeptAlertRuleSaveDto dto) {
        deptRegistrationBoardService.createAlertRule(dto);
        return ResultVo.ok();
    }

    @Operation(summary = "更新预警规则")
    @PutMapping("/alert-rules/{id}")
    public ResultVo<Void> updateAlertRule(@PathVariable Long id, @Valid @RequestBody DeptAlertRuleSaveDto dto) {
        deptRegistrationBoardService.updateAlertRule(id, dto);
        return ResultVo.ok();
    }

    @Operation(summary = "删除预警规则")
    @DeleteMapping("/alert-rules/{id}")
    public ResultVo<Void> deleteAlertRule(@PathVariable Long id) {
        deptRegistrationBoardService.deleteAlertRule(id);
        return ResultVo.ok();
    }
}
