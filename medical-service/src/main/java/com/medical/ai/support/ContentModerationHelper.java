package com.medical.ai.support;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 用户输入合规与急症关键词检测（优先于大模型调用）
 */
@Component
public class ContentModerationHelper {

    private static final List<String> EMERGENCY_KEYWORDS = List.of(
            "胸痛", "胸口痛", "胸疼", "心绞痛", "心肌梗死", "心脏病发作",
          "呼吸困难", "喘不过气", "窒息",
          "大出血", "大量出血", "吐血", "咯血",
          "意识不清", "昏迷", "晕厥", "抽搐", "惊厥",
          "自杀", "自残", "想死",
          "严重外伤", "车祸", "高处坠落",
          "中毒", "农药中毒",
            "120", "急救"
    );

    private static final String EMERGENCY_REPLY =
            "您描述的情况可能属于急症，请立即拨打 120 或尽快前往就近医院急诊科，"
                    + "不要等待线上咨询。本智能问诊仅提供健康参考，不能替代紧急救治。";

    /**
     * 检测用户输入是否命中急症关键词
     */
    public Optional<ModerationResult> checkEmergency(String userContent) {
        if (userContent == null || userContent.isBlank()) {
            return Optional.empty();
        }
        String normalized = userContent.toLowerCase(Locale.ROOT);
        for (String keyword : EMERGENCY_KEYWORDS) {
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return Optional.of(ModerationResult.emergency(EMERGENCY_REPLY, keyword));
            }
        }
        return Optional.empty();
    }

    /**
     * 规范化用户消息长度
     */
    public String normalizeUserContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (maxLength > 0 && trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }

    @Data
    public static class ModerationResult {
        private String urgencyLevel;
        private String reply;
        private String matchedKeyword;

        public static ModerationResult emergency(String reply, String keyword) {
            ModerationResult r = new ModerationResult();
            r.setUrgencyLevel("EMERGENCY");
            r.setReply(reply);
            r.setMatchedKeyword(keyword);
            return r;
        }
    }
}
