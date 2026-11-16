package com.studyloop.review.judge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 判分规则测试。
 * <p>
 * 这些用例就是"规则说明书"：以后谁改了判分逻辑，
 * 跑一遍就知道有没有破坏原来的约定。
 */
class AnswerJudgeTest {

    @Test
    @DisplayName("单选：选对 1 分，选错 0 分")
    void singleChoice() {
        List<String> correct = Collections.singletonList("A");
        assertEquals(1.0, AnswerJudge.judge("SINGLE", correct, Collections.singletonList("A")), 0.001);
        assertEquals(0.0, AnswerJudge.judge("SINGLE", correct, Collections.singletonList("B")), 0.001);
    }

    @Test
    @DisplayName("多选：全对 1 分，漏选半分，错选 0 分")
    void multipleChoice() {
        List<String> correct = Arrays.asList("A", "C");

        assertEquals(1.0, AnswerJudge.judge("MULTIPLE", correct, Arrays.asList("A", "C")), 0.001);
        assertEquals(1.0, AnswerJudge.judge("MULTIPLE", correct, Arrays.asList("C", "A")), 0.001);
        assertEquals(0.5, AnswerJudge.judge("MULTIPLE", correct, Collections.singletonList("A")), 0.001);
        assertEquals(0.0, AnswerJudge.judge("MULTIPLE", correct, Arrays.asList("A", "B")), 0.001);
        assertEquals(0.0, AnswerJudge.judge("MULTIPLE", correct, Arrays.asList("A", "B", "C")), 0.001);
    }

    @Test
    @DisplayName("填空：去空格、忽略大小写，多答案任一匹配")
    void blank() {
        List<String> correct = Arrays.asList("红黑树", "Red-Black Tree");

        assertEquals(1.0, AnswerJudge.judge("BLANK", correct, Collections.singletonList(" 红黑树 ")), 0.001);
        assertEquals(1.0, AnswerJudge.judge("BLANK", correct, Collections.singletonList("red-black tree")), 0.001);
        assertEquals(0.0, AnswerJudge.judge("BLANK", correct, Collections.singletonList("B树")), 0.001);
    }

    @Test
    @DisplayName("没作答一律 0 分")
    void emptyAnswer() {
        List<String> correct = Collections.singletonList("A");
        assertEquals(0.0, AnswerJudge.judge("SINGLE", correct, Collections.emptyList()), 0.001);
        assertEquals(0.0, AnswerJudge.judge("SINGLE", correct, null), 0.001);
    }

    @Test
    @DisplayName("主观题不参与自动判分")
    void subjectiveIsNotAutoGradable() {
        assertFalse(AnswerJudge.autoGradable("SUBJECTIVE"));
        assertFalse(AnswerJudge.autoGradable("IMAGE"));
        assertTrue(AnswerJudge.autoGradable("SINGLE"));
        assertTrue(AnswerJudge.autoGradable("BLANK"));
    }
}
