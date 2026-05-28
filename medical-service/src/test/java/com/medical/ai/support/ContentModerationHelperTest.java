package com.medical.ai.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentModerationHelperTest {

    private ContentModerationHelper helper;

    @BeforeEach
    void setUp() {
        helper = new ContentModerationHelper();
    }

    @Test
    void detectsEmergencyKeyword() {
        Optional<ContentModerationHelper.ModerationResult> result =
                helper.checkEmergency("我胸痛，呼吸困难");
        assertTrue(result.isPresent());
        assertEquals("EMERGENCY", result.get().getUrgencyLevel());
        assertTrue(result.get().getReply().contains("120"));
    }

    @Test
    void normalContentPasses() {
        assertTrue(helper.checkEmergency("头痛三天，有点低烧").isEmpty());
    }

    @Test
    void truncatesLongContent() {
        String longText = "a".repeat(100);
        assertEquals(50, helper.normalizeUserContent(longText, 50).length());
    }
}
