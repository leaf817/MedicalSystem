package com.medical.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 智能问诊会话
 */
@Data
@TableName("ai_consult_session")
public class AiConsultSession {

    @TableId(type = IdType.AUTO)
    private Long sessionId;

    private String sessionNo;
    private Long patientId;
    private String title;
    /** 1=进行中 2=已结束 3=已转预约 */
    private Integer status;
    private String chiefComplaint;
    private Long suggestedDeptId;
    private String urgencyLevel;
    private String summary;
    private Integer messageCount;
    private Integer tokenUsed;
    private Integer deleted;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private LocalDateTime endedTime;
}
