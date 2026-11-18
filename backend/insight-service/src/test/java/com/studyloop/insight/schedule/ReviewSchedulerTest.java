package com.studyloop.insight.schedule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReviewSchedulerTest {

    @Test
    @DisplayName("答对依次 1 → 3 → 7 → 14 天，封顶 14")
    void stagesAdvance() {
        assertEquals(1, ReviewScheduler.nextStage(0, true));
        assertEquals(2, ReviewScheduler.nextStage(1, true));
        assertEquals(3, ReviewScheduler.nextStage(2, true));
        assertEquals(3, ReviewScheduler.nextStage(3, true));
    }

    @Test
    @DisplayName("答错回到第一档")
    void wrongResets() {
        assertEquals(0, ReviewScheduler.nextStage(3, false));
        assertEquals(0, ReviewScheduler.nextStage(0, false));
    }

    @Test
    @DisplayName("下次复习日期按阶梯间隔")
    void nextDates() {
        LocalDate today = LocalDate.of(2026, 11, 17);
        assertEquals(today.plusDays(1), ReviewScheduler.nextDate(0, today));
        assertEquals(today.plusDays(3), ReviewScheduler.nextDate(1, today));
        assertEquals(today.plusDays(7), ReviewScheduler.nextDate(2, today));
        assertEquals(today.plusDays(14), ReviewScheduler.nextDate(3, today));
    }
}
