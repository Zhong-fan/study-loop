package com.studyloop.content.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeWeightCalculatorTest {

    @Test
    @DisplayName("重要度最高、有强调词和例题，权重接近满分")
    void fullSignals() {
        double weight = KnowledgeWeightCalculator.compute(5, 3, 2, 2, 5);
        assertEquals(1.0, weight, 0.001);
    }

    @Test
    @DisplayName("没有任何证据时只剩重要度贡献")
    void importanceOnly() {
        double weight = KnowledgeWeightCalculator.compute(3, 0, 0, 0, 0);
        assertEquals(0.21, weight, 0.001);
    }

    @Test
    @DisplayName("强调词最多按 3 次计，再多也不涨")
    void keywordCapped() {
        double three = KnowledgeWeightCalculator.compute(3, 3, 0, 0, 0);
        double ten = KnowledgeWeightCalculator.compute(3, 10, 0, 0, 0);
        assertEquals(three, ten, 0.0001);
    }

    @Test
    @DisplayName("异常输入被夹到合法区间")
    void clampsInput() {
        double weight = KnowledgeWeightCalculator.compute(99, -5, -1, -3, -2);
        assertTrue(weight >= 0 && weight <= 1);
    }
}
