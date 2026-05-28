package com.medical.web.api.patient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.pagination.PageResult;
import com.medical.common.response.ResultVo;
import com.medical.domain.dto.AiConsultMessageSendDto;
import com.medical.domain.entity.Patient;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.*;
import com.medical.mapper.PatientMapper;
import com.medical.mapper.SysUserMapper;
import com.medical.service.AiConsultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 患者端 — AI 智能问诊
 */
@Tag(name = "患者端-智能问诊")
@RestController
@RequestMapping("/api/patient/ai-consult")
@RequiredArgsConstructor
public class PatientAiConsultController {

    private final AiConsultService aiConsultService;
    private final PatientMapper patientMapper;
    private final SysUserMapper sysUserMapper;

    @Operation(summary = "创建问诊会话", description = "返回 sessionId、欢迎语与免责声明")
    @PostMapping("/session")
    public ResultVo<AiConsultSessionCreateVo> createSession() {
        return ResultVo.ok(aiConsultService.createSession(getCurrentPatientId()));
    }

    @Operation(summary = "我的问诊会话列表（分页）")
    @GetMapping("/session/list")
    public ResultVo<PageResult<AiConsultSessionListVo>> listSessions(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") long pageSize) {
        return ResultVo.ok(aiConsultService.listSessions(getCurrentPatientId(), pageNum, pageSize));
    }

    @Operation(summary = "会话详情（含消息列表）")
    @GetMapping("/session/{sessionId}")
    public ResultVo<AiConsultSessionDetailVo> getSessionDetail(
            @Parameter(description = "会话 ID") @PathVariable Long sessionId) {
        return ResultVo.ok(aiConsultService.getSessionDetail(sessionId, getCurrentPatientId()));
    }

    @Operation(summary = "发送消息（同步返回 AI 回复）")
    @PostMapping("/session/{sessionId}/message")
    public ResultVo<AiConsultSendMessageVo> sendMessage(
            @Parameter(description = "会话 ID") @PathVariable Long sessionId,
            @Valid @RequestBody AiConsultMessageSendDto dto) {
        return ResultVo.ok(aiConsultService.sendMessage(sessionId, getCurrentPatientId(), dto.getContent()));
    }

    @Operation(summary = "结束问诊并生成摘要")
    @PutMapping("/session/{sessionId}/end")
    public ResultVo<AiConsultSummaryVo> endSession(
            @Parameter(description = "会话 ID") @PathVariable Long sessionId) {
        return ResultVo.ok(aiConsultService.endSession(sessionId, getCurrentPatientId()));
    }

    @Operation(summary = "获取问诊摘要（仅已结束会话）")
    @GetMapping("/session/{sessionId}/summary")
    public ResultVo<AiConsultSummaryVo> getSummary(
            @Parameter(description = "会话 ID") @PathVariable Long sessionId) {
        return ResultVo.ok(aiConsultService.getSummary(sessionId, getCurrentPatientId()));
    }

    @Operation(summary = "删除问诊会话（软删）")
    @DeleteMapping("/session/{sessionId}")
    public ResultVo<Void> deleteSession(
            @Parameter(description = "会话 ID") @PathVariable Long sessionId) {
        aiConsultService.deleteSession(sessionId, getCurrentPatientId());
        return ResultVo.ok();
    }

    @Operation(summary = "根据科室名称关键词匹配 dept_id")
    @GetMapping("/dept/suggest")
    public ResultVo<Long> suggestDept(
            @Parameter(description = "科室名称关键词") @RequestParam String keyword) {
        return ResultVo.ok(aiConsultService.suggestDeptId(keyword));
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
