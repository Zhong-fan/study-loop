package com.studyloop.insight.schedule;

import java.time.LocalDate;

/**
 * 复习排期：固定 1 / 3 / 7 / 14 天四档。
 * <p>
 * 答对进一档，答错回到第一档。比 SM-2 简单，但和「1/3/7/14」的教学节奏一致，
 * 现场也容易解释：第一次明天看，第二次三天后，第三次一周后，之后两周一次。
 */
public class ReviewScheduler {

    public static final int[] INTERVALS = {1, 3, 7, 14};

    private ReviewScheduler() {
    }

    /** 答对升一档（最高第 4 档），答错打回第 1 档 */
    public static int nextStage(int stage, boolean correct) {
        if (!correct) {
            return 0;
        }
        int next = stage + 1;
        return Math.min(next, INTERVALS.length - 1);
    }

    public static LocalDate nextDate(int stage, LocalDate today) {
        int safe = Math.max(0, Math.min(stage, INTERVALS.length - 1));
        return today.plusDays(INTERVALS[safe]);
    }
}
