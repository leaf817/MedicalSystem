package com.medical.web.api.nurse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.response.ResultVo;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.PrescriptionVo;
import com.medical.mapper.SysUserMapper;
import com.medical.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "药房端-处方管理")
@RestController
@RequestMapping("/api/nurse/prescription")
@RequiredArgsConstructor
public class NursePrescriptionController {

    private final PrescriptionService prescriptionService;
    private final SysUserMapper sysUserMapper;

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
        prescriptionService.dispensePrescription(id, getCurrentUserId());
        return ResultVo.ok();
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

    @Operation(summary = "处方详情")
    @GetMapping("/{id}")
    public ResultVo<PrescriptionVo> detail(@PathVariable Long id) {
        PrescriptionVo vo = prescriptionService.getPrescriptionDetail(id);
        return ResultVo.ok(vo);
    }
}