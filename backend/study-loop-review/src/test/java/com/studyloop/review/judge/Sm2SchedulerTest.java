package com.studyloop.review.judge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SM-2 排期测试。
 * <p>
 * 重点是"三次答对的间隔"和"答错之后怎么回到起点"，
 * 这两个行为是答辩时最容易被追问的。
 */
class Sm2SchedulerTest {

    private static final double DELTA = 0.001;

    @Test
    @DisplayName("新卡连续答对：1 天 → 6 天 → 间隔乘以难度系数")
    void intervalsGrow() {
        // 第一次答对
        Sm2Scheduler.Result first = Sm2Scheduler.next(2, 2.5, 0, 0, 0);
        assertEquals(1, first.intervalDays);
        assertEquals(1, first.repetitions);

        // 第二次答对
        Sm2Scheduler.Result second = Sm2Scheduler.next(2, first.ease, first.intervalDays, first.repetitions, first.lapses);
        assertEquals(6, second.intervalDays);
        assertEquals(2, second.repetitions);

        // 第三次答对：6 * ease
        Sm2Scheduler.Result third = Sm2Scheduler.next(2, second.ease, second.intervalDays, second.repetitions, second.lapses);
        assertTrue(third.intervalDays > 6);
        assertEquals(3, third.repetitions);
    }

    @Test
    @DisplayName("答错：连续次数归零，明天再来，遗忘次数加一")
    void forgotResets() {
        Sm2Scheduler.Result result = Sm2Scheduler.next(0, 2.5, 30, 4, 0);

        assertEquals(1, result.intervalDays);
        assertEquals(0, result.repetitions);
        assertEquals(1, result.lapses);
    }

    @Test
    @DisplayName("难度系数不会跌破 1.3，也不会高过 2.8")
    void easeIsClamped() {
        Sm2Scheduler.Result low = Sm2Scheduler.next(0, 1.3, 10, 2, 3);
        assertTrue(low.ease >= 1.3 - DELTA);

        Sm2Scheduler.Result high = Sm2Scheduler.next(2, 2.8, 10, 2, 0);
        assertTrue(high.ease <= 2.8 + DELTA);
    }

    @Test
    @DisplayName("间隔有上限，不会算到几年以后")
    void intervalIsCapped() {
        Sm2Scheduler.Result result = Sm2Scheduler.next(2, 2.8, 300, 9, 0);
        assertTrue(result.intervalDays <= 365);
    }
}
