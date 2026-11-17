package com.studyloop.insight.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasteryRuleTest {

    @Test
    @DisplayName("单点高、综合低，判为会背不会用")
    void transferDetected() {
        assertTrue(MasteryRule.isTransfer(1.0, 2, 0.0, 1));
    }

    @Test
    @DisplayName("样本不足时不轻易下结论")
    void needsSamples() {
        assertFalse(MasteryRule.isTransfer(1.0, 1, 0.0, 1));
        assertFalse(MasteryRule.isTransfer(1.0, 2, 0.0, 0));
    }

    @Test
    @DisplayName("差距不到阈值不算迁移薄弱")
    void smallGapIgnored() {
        assertFalse(MasteryRule.isTransfer(0.9, 2, 0.7, 2));
    }

    @Test
    @DisplayName("会背不会用时不升档")
    void transferBlocksAdvance() {
        assertEquals(1, MasteryRule.nextStage(1, 1.0, true));
    }

    @Test
    @DisplayName("答对且不迁移薄弱时升档")
    void correctAdvances() {
        assertEquals(2, MasteryRule.nextStage(1, 1.0, false));
    }

    @Test
    @DisplayName("低于半分归零，中间分保持")
    void partialAndWrong() {
        assertEquals(0, MasteryRule.nextStage(2, 0.0, false));
        assertEquals(2, MasteryRule.nextStage(2, 0.5, false));
    }
}
