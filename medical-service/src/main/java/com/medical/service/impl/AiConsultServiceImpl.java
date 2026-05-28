package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.ai.client.LlmClient;
import com.medical.ai.client.dto.LlmChatMessage;
import com.medical.ai.client.dto.LlmChatResult;
import com.medical.ai.config.AiProperties;
import com.medical.ai.prompt.PromptTemplateService;
import com.medical.ai.support.AiConsultSummaryParser;
import com.medical.ai.support.AiRateLimitService;
import com.medical.ai.support.ContentModerationHelper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.pagination.PageResult;
import com.medical.domain.entity.AiConsultMessage;
import com.medical.domain.entity.AiConsultSession;
import com.medical.domain.entity.SysDept;
import com.medical.domain.vo.*;
import com.medical.mapper.AiConsultMessageMapper;
import com.medical.mapper.AiConsultSessionMapper;
import com.medical.mapper.SysDeptMapper;
import com.medical.service.AiConsultService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiConsultServiceImpl implements AiConsultService {

    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_ENDED = 2;
    private static final DateTimeFormatter SESSION_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AiConsultSessionMapper sessionMapper;
    private final AiConsultMessageMapper messageMapper;
    private final SysDeptMapper sysDeptMapper;
    private final LlmClient llmClient;
    private final AiProperties aiProperties;
    private final PromptTemplateService promptTemplateService;
    private final ContentModerationHelper contentModerationHelper;
    private final AiRateLimitService aiRateLimitService;
    private final AiConsultSummaryParser summaryParser;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiConsultSessionCreateVo createSession(Long patientId) {
        LocalDateTime now = LocalDateTime.now();
        AiConsultSession session = new AiConsultSession();
        session.setSessionNo(generateSessionNo());
        session.setPatientId(patientId);
        session.setTitle("新问诊");
        session.setStatus(STATUS_ACTIVE);
        session.setUrgencyLevel("NORMAL");
        session.setMessageCount(0);
        session.setTokenUsed(0);
        session.setDeleted(0);
        session.setCreatedTime(now);
        session.setUpdatedTime(now);
        sessionMapper.insert(session);

        String systemPrompt = promptTemplateService.buildConsultSystemPrompt(loadActiveDepartments());
        insertMessage(session.getSessionId(), "system", systemPrompt, null, 0, 0, now);

        String welcome = promptTemplateService.getWelcomeMessage();
        insertMessage(session.getSessionId(), "assistant", welcome, "mock-welcome", 0, 0, now);

        session.setMessageCount(2);
        sessionMapper.updateById(session);

        AiConsultSessionCreateVo vo = new AiConsultSessionCreateVo();
        vo.setSessionId(session.getSessionId());
        vo.setSessionNo(session.getSessionNo());
        vo.setWelcomeMessage(welcome);
        vo.setDisclaimer(promptTemplateService.getDisclaimer());
        return vo;
    }

    @Override
    public PageResult<AiConsultSessionListVo> listSessions(Long patientId, long pageNum, long pageSize) {
        LambdaQueryWrapper<AiConsultSession> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(AiConsultSession::getPatientId, patientId)
                .eq(AiConsultSession::getDeleted, 0)
                .orderByDesc(AiConsultSession::getCreatedTime);

        Page<AiConsultSession> page = sessionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Page<AiConsultSessionListVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toListVo).collect(Collectors.toList()));
        return PageResult.fromPage(voPage);
    }

    @Override
    public AiConsultSessionDetailVo getSessionDetail(Long sessionId, Long patientId) {
        AiConsultSession session = requireActiveSession(sessionId, patientId, false);
        AiConsultSessionDetailVo vo = new AiConsultSessionDetailVo();
        BeanUtils.copyProperties(session, vo);
        vo.setMessages(listMessageVos(sessionId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiConsultSendMessageVo sendMessage(Long sessionId, Long patientId, String content) {
        AiConsultSession session = requireActiveSession(sessionId, patientId, true);
        aiRateLimitService.checkAndIncrement(patientId);

        String normalized = contentModerationHelper.normalizeUserContent(
                content, aiProperties.getMaxUserMessageLength());
        if (normalized.isBlank()) {
            throw new BusinessWarningException("消息内容不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        AiConsultSendMessageVo vo = new AiConsultSendMessageVo();

        Optional<ContentModerationHelper.ModerationResult> emergency =
                contentModerationHelper.checkEmergency(normalized);
        AiConsultMessage userMsg = insertMessage(sessionId, "user", normalized, null, 0, 0, now);
        vo.setUserMessageId(userMsg.getMessageId());

        String reply;
        String modelName;
        int promptTokens = 0;
        int completionTokens = 0;
        String urgencyHint = null;

        if (emergency.isPresent()) {
            reply = emergency.get().getReply();
            modelName = "rule-emergency";
            urgencyHint = emergency.get().getUrgencyLevel();
            session.setUrgencyLevel(urgencyHint);
        } else {
            List<LlmChatMessage> llmMessages = buildLlmContextMessages(sessionId);
            LlmChatResult result = llmClient.chat(llmMessages);
            reply = result.getContent();
            modelName = result.getModel();
            promptTokens = result.getPromptTokens() != null ? result.getPromptTokens() : 0;
            completionTokens = result.getCompletionTokens() != null ? result.getCompletionTokens() : 0;
        }

        AiConsultMessage assistantMsg = insertMessage(
                sessionId, "assistant", reply, modelName, promptTokens, completionTokens, now);
        vo.setAssistantMessageId(assistantMsg.getMessageId());
        vo.setReply(reply);
        vo.setUrgencyHint(urgencyHint);

        if (session.getTitle() == null || "新问诊".equals(session.getTitle())) {
            session.setTitle(abbreviate(normalized, 30));
        }
        session.setMessageCount(safeCount(session.getMessageCount()) + 2);
        session.setTokenUsed(safeCount(session.getTokenUsed()) + promptTokens + completionTokens);
        session.setUpdatedTime(now);
        sessionMapper.updateById(session);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiConsultSummaryVo endSession(Long sessionId, Long patientId) {
        AiConsultSession session = requireActiveSession(sessionId, patientId, true);

        String transcript = buildConversationTranscript(sessionId);
        List<LlmChatMessage> messages = List.of(
                LlmChatMessage.system(promptTemplateService.buildSummarySystemPrompt()),
                LlmChatMessage.user(promptTemplateService.buildSummaryUserPrompt(transcript))
        );

        LlmChatResult result = llmClient.chat(messages);
        AiConsultSummaryVo summaryVo = summaryParser.parse(result.getContent());
        applySummaryToSession(session, summaryVo, result.getContent());

        LocalDateTime now = LocalDateTime.now();
        session.setStatus(STATUS_ENDED);
        session.setEndedTime(now);
        session.setUpdatedTime(now);
        session.setTokenUsed(safeCount(session.getTokenUsed())
                + safeCount(result.getPromptTokens()) + safeCount(result.getCompletionTokens()));
        sessionMapper.updateById(session);

        summaryVo.setSessionId(sessionId);
        return summaryVo;
    }

    @Override
    public AiConsultSummaryVo getSummary(Long sessionId, Long patientId) {
        AiConsultSession session = requireActiveSession(sessionId, patientId, false);
        if (session.getStatus() == null || session.getStatus() == STATUS_ACTIVE) {
            throw new BusinessWarningException("会话尚未结束，请先结束问诊");
        }
        return buildSummaryVo(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId, Long patientId) {
        AiConsultSession session = requireActiveSession(sessionId, patientId, false);
        session.setDeleted(1);
        session.setUpdatedTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    @Override
    public Long suggestDeptId(String deptNameKeyword) {
        if (deptNameKeyword == null || deptNameKeyword.isBlank()) {
            return null;
        }
        return matchDeptByName(deptNameKeyword.trim());
    }

    private void applySummaryToSession(AiConsultSession session, AiConsultSummaryVo summaryVo, String rawJson) {
        session.setChiefComplaint(summaryVo.getChiefComplaint());
        if (summaryVo.getUrgencyLevel() != null && !summaryVo.getUrgencyLevel().isBlank()) {
            session.setUrgencyLevel(summaryVo.getUrgencyLevel());
        }
        session.setSummary(rawJson);
        Long deptId = matchDeptByName(summaryVo.getSuggestedDeptName());
        summaryVo.setSuggestedDeptId(deptId);
        session.setSuggestedDeptId(deptId);
        summaryVo.setSuggestedDeptName(summaryVo.getSuggestedDeptName());
    }

    private AiConsultSummaryVo buildSummaryVo(AiConsultSession session) {
        AiConsultSummaryVo vo = summaryParser.parse(session.getSummary());
        vo.setSessionId(session.getSessionId());
        vo.setChiefComplaint(firstNonBlank(vo.getChiefComplaint(), session.getChiefComplaint()));
        vo.setUrgencyLevel(firstNonBlank(vo.getUrgencyLevel(), session.getUrgencyLevel()));
        vo.setSuggestedDeptId(session.getSuggestedDeptId());
        if (vo.getSuggestedDeptName() == null && session.getSuggestedDeptId() != null) {
            SysDept dept = sysDeptMapper.selectById(session.getSuggestedDeptId());
            if (dept != null) {
                vo.setSuggestedDeptName(dept.getName());
            }
        }
        return vo;
    }

    private Long matchDeptByName(String deptName) {
        if (deptName == null || deptName.isBlank()) {
            return null;
        }
        String target = deptName.trim();
        List<SysDept> depts = loadActiveDepartments();
        for (SysDept dept : depts) {
            if (dept.getName() == null) {
                continue;
            }
            if (dept.getName().equals(target)
                    || dept.getName().contains(target)
                    || target.contains(dept.getName())) {
                return dept.getDeptId();
            }
        }
        return null;
    }

    private List<SysDept> loadActiveDepartments() {
        LambdaQueryWrapper<SysDept> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysDept::getStatus, 1)
                .lt(SysDept::getDeptId, 20)
                .orderByAsc(SysDept::getSortOrder);
        return sysDeptMapper.selectList(wrapper);
    }

    private List<LlmChatMessage> buildLlmContextMessages(Long sessionId) {
        List<AiConsultMessage> all = listMessagesAsc(sessionId);
        LlmChatMessage system = null;
        List<LlmChatMessage> dialog = new ArrayList<>();

        for (AiConsultMessage m : all) {
            if ("system".equals(m.getRole())) {
                system = new LlmChatMessage(m.getRole(), m.getContent());
            } else if ("user".equals(m.getRole()) || "assistant".equals(m.getRole())) {
                if (!"mock-welcome".equals(m.getModelName())) {
                    dialog.add(new LlmChatMessage(m.getRole(), m.getContent()));
                }
            }
        }

        int max = aiProperties.getMaxContextMessages();
        if (max > 0 && dialog.size() > max) {
            dialog = dialog.subList(dialog.size() - max, dialog.size());
        }

        List<LlmChatMessage> result = new ArrayList<>();
        if (system != null) {
            result.add(system);
        }
        result.addAll(dialog);
        return result;
    }

    private String buildConversationTranscript(Long sessionId) {
        return listMessagesAsc(sessionId).stream()
                .filter(m -> "user".equals(m.getRole()) || "assistant".equals(m.getRole()))
                .filter(m -> !"mock-welcome".equals(m.getModelName()))
                .map(m -> {
                    String roleLabel = "user".equals(m.getRole()) ? "患者" : "助手";
                    return roleLabel + "：" + m.getContent();
                })
                .collect(Collectors.joining("\n"));
    }

    private AiConsultSession requireActiveSession(Long sessionId, Long patientId, boolean mustBeActive) {
        AiConsultSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getDeleted() != null && session.getDeleted() == 1) {
            throw new BusinessWarningException("问诊会话不存在");
        }
        if (!patientId.equals(session.getPatientId())) {
            throw new BusinessWarningException("无权访问该问诊会话");
        }
        if (mustBeActive && (session.getStatus() == null || session.getStatus() != STATUS_ACTIVE)) {
            throw new BusinessWarningException("会话已结束，请新建会话");
        }
        return session;
    }

    private AiConsultMessage insertMessage(Long sessionId, String role, String content, String modelName,
                                           int promptTokens, int completionTokens, LocalDateTime time) {
        AiConsultMessage msg = new AiConsultMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setContentType("TEXT");
        msg.setModelName(modelName);
        msg.setPromptTokens(promptTokens);
        msg.setCompletionTokens(completionTokens);
        msg.setCreatedTime(time);
        messageMapper.insert(msg);
        return msg;
    }

    private List<AiConsultMessage> listMessagesAsc(Long sessionId) {
        LambdaQueryWrapper<AiConsultMessage> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(AiConsultMessage::getSessionId, sessionId)
                .orderByAsc(AiConsultMessage::getCreatedTime);
        return messageMapper.selectList(wrapper);
    }

    private List<AiConsultMessageVo> listMessageVos(Long sessionId) {
        return listMessagesAsc(sessionId).stream().map(m -> {
            AiConsultMessageVo vo = new AiConsultMessageVo();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    private AiConsultSessionListVo toListVo(AiConsultSession session) {
        AiConsultSessionListVo vo = new AiConsultSessionListVo();
        BeanUtils.copyProperties(session, vo);
        return vo;
    }

    private static String generateSessionNo() {
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "C" + LocalDateTime.now().format(SESSION_NO_FMT) + suffix;
    }

    private static int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private static String abbreviate(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…";
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }
}
