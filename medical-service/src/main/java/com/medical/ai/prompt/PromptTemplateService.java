package com.medical.ai.prompt;

import com.medical.domain.entity.SysDept;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 问诊系统 Prompt 与固定文案
 */
@Service
public class PromptTemplateService {

    public static final String DISCLAIMER =
            "本服务由人工智能提供健康咨询参考，不能替代执业医师面诊。"
                    + "如有胸痛、呼吸困难、大出血、意识不清等紧急情况，请立即拨打 120 或前往急诊科。";

    private static final String WELCOME_MESSAGE =
            "您好，我是智能问诊助手。请描述您哪里不舒服、症状持续了多久。"
                    + "我会通过几个问题帮助您梳理情况，并给出就医科室建议（不能替代医生诊断）。";

    public String getWelcomeMessage() {
        return WELCOME_MESSAGE;
    }

    public String getDisclaimer() {
        return DISCLAIMER;
    }

    /**
     * 多轮对话系统 Prompt（注入科室列表）
     */
    public String buildConsultSystemPrompt(List<SysDept> departments) {
        String deptLines = departments.stream()
                .map(d -> "- dept_id=" + d.getDeptId() + ", name=" + d.getName())
                .collect(Collectors.joining("\n"));

        return """
                你是「智能医疗服务管理系统」的线上预问诊助手，面向患者使用简体中文。
                
                【任务】
                通过简短、有同理心的追问，了解主诉、起病时间、严重程度、诱因、伴随症状；
                可询问过敏史、既往史（患者愿意时）。
                
                【输出约束】
                1. 每次回复控制在 150 字以内；
                2. 禁止给出明确确诊（不要说「您得了XXX病」）；
                3. 可建议「建议到XX科室就诊」「建议尽快就医」；
                4. 若涉及急症（胸痛、昏迷、大出血、呼吸困难等），首句提醒拨打120或去急诊；
                5. 不要开具处方或具体用药剂量。
                
                【本院科室列表（摘要建议科室时请使用下列名称）】
                %s
                
                【结束】
                若患者表示没有补充，可提示其点击「结束问诊」生成摘要。
                """.formatted(deptLines.isEmpty() ? "（暂无科室数据）" : deptLines);
    }

    /**
     * 结束会话时的摘要 Prompt（要求模型仅输出 JSON）
     */
    public String buildSummaryUserPrompt(String conversationTranscript) {
        return """
                请根据以下医患对话记录，仅输出一个 JSON 对象，不要 markdown 代码块，不要其他说明：
                {
                  "chief_complaint": "一句话主诉",
                  "urgency_level": "NORMAL或URGENT或EMERGENCY",
                  "suggested_dept_name": "建议科室名称，须来自对话或科室列表",
                  "medical_advice": "就医建议，100字内",
                  "questions_for_doctor": ["建议医生进一步了解的问题1", "问题2"]
                }
                
                对话记录：
                %s
                """.formatted(conversationTranscript);
    }

    public String buildSummarySystemPrompt() {
        return "你是医疗预问诊摘要助手，只输出合法 JSON，字段名必须与要求一致。";
    }
}
