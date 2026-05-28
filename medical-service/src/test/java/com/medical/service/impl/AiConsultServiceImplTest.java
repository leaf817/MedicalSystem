package com.medical.service.impl;

import com.medical.ai.client.LlmClient;
import com.medical.ai.client.dto.LlmChatResult;
import com.medical.ai.config.AiProperties;
import com.medical.ai.prompt.PromptTemplateService;
import com.medical.ai.support.AiConsultSummaryParser;
import com.medical.ai.support.AiRateLimitService;
import com.medical.ai.support.ContentModerationHelper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.domain.entity.AiConsultSession;
import com.medical.domain.entity.SysDept;
import com.medical.domain.vo.AiConsultSendMessageVo;
import com.medical.domain.vo.AiConsultSessionCreateVo;
import com.medical.domain.vo.AiConsultSummaryVo;
import com.medical.mapper.AiConsultMessageMapper;
import com.medical.mapper.AiConsultSessionMapper;
import com.medical.mapper.SysDeptMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiConsultServiceImplTest {

    @Mock
    private AiConsultSessionMapper sessionMapper;
    @Mock
    private AiConsultMessageMapper messageMapper;
    @Mock
    private SysDeptMapper sysDeptMapper;
    @Mock
    private LlmClient llmClient;
    @Mock
    private AiRateLimitService aiRateLimitService;

    @Spy
    private AiProperties aiProperties = new AiProperties();

    @Spy
    private PromptTemplateService promptTemplateService = new PromptTemplateService();

    @Spy
    private ContentModerationHelper contentModerationHelper = new ContentModerationHelper();

    @Spy
    private AiConsultSummaryParser summaryParser = new AiConsultSummaryParser(new ObjectMapper());

    @InjectMocks
    private AiConsultServiceImpl aiConsultService;

    @BeforeEach
    void setUp() {
        SysDept dept = new SysDept();
        dept.setDeptId(1L);
        dept.setName("内科");
        dept.setStatus(1);
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(dept));

        doAnswer(inv -> {
            AiConsultSession s = inv.getArgument(0);
            if (s.getSessionId() == null) {
                s.setSessionId(100L);
            }
            return 1;
        }).when(sessionMapper).insert(any(AiConsultSession.class));

        doAnswer(inv -> {
            com.medical.domain.entity.AiConsultMessage m = inv.getArgument(0);
            if (m.getMessageId() == null) {
                m.setMessageId(System.nanoTime());
            }
            return 1;
        }).when(messageMapper).insert(any(com.medical.domain.entity.AiConsultMessage.class));
    }

    @Test
    void createSessionReturnsWelcome() {
        AiConsultSessionCreateVo vo = aiConsultService.createSession(1L);
        assertNotNull(vo.getSessionId());
        assertTrue(vo.getSessionNo().startsWith("C"));
        assertNotNull(vo.getWelcomeMessage());
        assertNotNull(vo.getDisclaimer());
    }

    @Test
    void sendMessageEmergencySkipsLlm() {
        AiConsultSession session = activeSession(1L, 1L);
        when(sessionMapper.selectById(1L)).thenReturn(session);

        AiConsultSendMessageVo vo = aiConsultService.sendMessage(1L, 1L, "我胸痛，呼吸困难");
        assertTrue(vo.getReply().contains("120"));
        assertEquals("EMERGENCY", vo.getUrgencyHint());
        verify(llmClient, org.mockito.Mockito.never()).chat(any());
    }

    @Test
    void sendMessageCallsLlm() {
        AiConsultSession session = activeSession(1L, 1L);
        when(sessionMapper.selectById(1L)).thenReturn(session);

        com.medical.domain.entity.AiConsultMessage system = new com.medical.domain.entity.AiConsultMessage();
        system.setRole("system");
        system.setContent("system prompt");
        com.medical.domain.entity.AiConsultMessage user = new com.medical.domain.entity.AiConsultMessage();
        user.setRole("user");
        user.setContent("头痛三天");
        when(messageMapper.selectList(any())).thenReturn(List.of(system, user));

        when(llmClient.chat(any())).thenReturn(LlmChatResult.builder()
                .content("请问头痛多久了？")
                .model("glm-4-flash")
                .promptTokens(10)
                .completionTokens(5)
                .build());

        AiConsultSendMessageVo vo = aiConsultService.sendMessage(1L, 1L, "头痛三天");
        assertEquals("请问头痛多久了？", vo.getReply());
        verify(llmClient).chat(any());
    }

    @Test
    void endSessionRejectedWhenNotOwner() {
        AiConsultSession session = activeSession(1L, 2L);
        when(sessionMapper.selectById(1L)).thenReturn(session);
        assertThrows(BusinessWarningException.class,
                () -> aiConsultService.endSession(1L, 1L));
    }

    @Test
    void suggestDeptIdMatchesName() {
        Long id = aiConsultService.suggestDeptId("内科");
        assertEquals(1L, id);
    }

    private static AiConsultSession activeSession(Long sessionId, Long patientId) {
        AiConsultSession session = new AiConsultSession();
        session.setSessionId(sessionId);
        session.setPatientId(patientId);
        session.setStatus(1);
        session.setDeleted(0);
        session.setMessageCount(2);
        session.setTokenUsed(0);
        session.setTitle("新问诊");
        return session;
    }
}
