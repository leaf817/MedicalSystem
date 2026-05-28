package com.medical.ai.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medical.domain.vo.AiConsultSummaryVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析结束问诊时模型返回的 JSON 摘要
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiConsultSummaryParser {

    private final ObjectMapper objectMapper;

    public AiConsultSummaryVo parse(String raw) {
        AiConsultSummaryVo vo = new AiConsultSummaryVo();
        if (raw == null || raw.isBlank()) {
            return vo;
        }
        String json = stripMarkdownFence(raw.trim());
        try {
            JsonNode node = objectMapper.readTree(json);
            vo.setChiefComplaint(text(node, "chief_complaint"));
            vo.setUrgencyLevel(text(node, "urgency_level"));
            vo.setSuggestedDeptName(text(node, "suggested_dept_name"));
            vo.setMedicalAdvice(text(node, "medical_advice"));
            vo.setQuestionsForDoctor(readStringArray(node.get("questions_for_doctor")));
            vo.setRawSummary(json);
            return vo;
        } catch (Exception e) {
            log.warn("Failed to parse AI summary JSON, fallback to raw text: {}", e.getMessage());
            vo.setRawSummary(raw);
            vo.setMedicalAdvice(raw.length() > 500 ? raw.substring(0, 500) : raw);
            vo.setUrgencyLevel("NORMAL");
            return vo;
        }
    }

    private static String stripMarkdownFence(String s) {
        if (s.startsWith("```")) {
            int start = s.indexOf('\n');
            int end = s.lastIndexOf("```");
            if (start > 0 && end > start) {
                return s.substring(start + 1, end).trim();
            }
        }
        return s;
    }

    private static String text(JsonNode node, String field) {
        JsonNode f = node.get(field);
        return f == null || f.isNull() ? null : f.asText();
    }

    private static List<String> readStringArray(JsonNode arr) {
        List<String> list = new ArrayList<>();
        if (arr == null || !arr.isArray()) {
            return list;
        }
        arr.forEach(n -> {
            if (n != null && !n.isNull()) {
                list.add(n.asText());
            }
        });
        return list;
    }
}
