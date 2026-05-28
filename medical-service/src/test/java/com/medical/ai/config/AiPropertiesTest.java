package com.medical.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "medical.ai.provider=zhipu",
        "medical.ai.model=glm-4-flash",
        "medical.ai.max-context-messages=20"
})
class AiPropertiesTest {

    @Autowired
    private AiProperties aiProperties;

    @Test
    void loadsMedicalAiProperties() {
        assertEquals("zhipu", aiProperties.getProvider());
        assertEquals("glm-4-flash", aiProperties.getModel());
        assertEquals(20, aiProperties.getMaxContextMessages());
        assertTrue(aiProperties.getBaseUrl().contains("open.bigmodel.cn"));
    }
}
