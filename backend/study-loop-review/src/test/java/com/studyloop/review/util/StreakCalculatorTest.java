package com.studyloop.review.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 连续天数测试。
 * <p>
 * 边界都在这：今天没复习但昨天复习了、断签、完全没复习。
 */
class StreakCalculatorTest {

    private final LocalDate today = LocalDate.of(2026, 11, 16);

    @Test
    @DisplayName("连续三天，含今天")
    void threeDaysIncludingToday() {
        List<LocalDate> dates = Arrays.asList(today, today.minusDays(1), today.minusDays(2));
        assertEquals(3, StreakCalculator.calculate(dates, today));
    }

    @Test
    @DisplayName("今天还没复习，从昨天开始数，不算断签")
    void startsFromYesterday() {
        List<LocalDate> dates = Arrays.asList(today.minusDays(1), today.minusDays(2));
        assertEquals(2, StreakCalculator.calculate(dates, today));
    }

    @Test
    @DisplayName("中间断了一天，只算最近的连续段")
    void breaksInTheMiddle() {
        List<LocalDate> dates = Arrays.asList(today, today.minusDays(1), today.minusDays(3));
        assertEquals(2, StreakCalculator.calculate(dates, today));
    }

    @Test
    @DisplayName("很久没复习了，连续天数为 0")
    void staleStreak() {
        assertEquals(0, StreakCalculator.calculate(Collections.singletonList(today.minusDays(5)), today));
    }

    @Test
    @DisplayName("没有任何记录")
    void empty() {
        assertEquals(0, StreakCalculator.calculate(Collections.emptyList(), today));
    }
}
