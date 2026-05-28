package com.medical.service;

import com.medical.common.pagination.PageResult;
import com.medical.domain.vo.*;

public interface AiConsultService {

    AiConsultSessionCreateVo createSession(Long patientId);

    PageResult<AiConsultSessionListVo> listSessions(Long patientId, long pageNum, long pageSize);

    AiConsultSessionDetailVo getSessionDetail(Long sessionId, Long patientId);

    AiConsultSendMessageVo sendMessage(Long sessionId, Long patientId, String content);

    AiConsultSummaryVo endSession(Long sessionId, Long patientId);

    AiConsultSummaryVo getSummary(Long sessionId, Long patientId);

    void deleteSession(Long sessionId, Long patientId);

    /**
     * 根据科室名称关键词匹配 dept_id（规则）
     */
    Long suggestDeptId(String deptNameKeyword);
}
