package com.studyloop.practice.judge;

/**
 * SM-2 排期算法（简化版）。
 * <p>
 * 三个输入：这次评了几分、原来的难度系数、原来间隔多久。
 * 三个输出：新的难度系数、新间隔、新连续答对次数。
 * <p>
 * 评分：0 忘记 / 1 模糊 / 2 记住。
 */
public class Sm2Scheduler {

    /** 间隔上限，防止越滚越大 */
    private static final int MAX_INTERVAL_DAYS = 365;

    private Sm2Scheduler() {
    }

    public static Result next(int rating, double ease, int intervalDays, int repetitions, int lapses) {
        // 把三档评分映射成 SM-2 原来的 0~5 分
        int q = 5;
        if (rating <= 0) {
            q = 0;
        } else if (rating == 1) {
            q = 3;
        }

        double newEase = ease + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        if (newEase < 1.3) {
            newEase = 1.3;
        }
        if (newEase > 2.8) {
            newEase = 2.8;
        }

        Result result = new Result();
        result.ease = newEase;

        if (q < 3) {
            // 忘了：重新开始，明天再来
            result.repetitions = 0;
            result.lapses = lapses + 1;
            result.intervalDays = 1;
        } else {
            result.repetitions = repetitions + 1;
            result.lapses = lapses;

            if (result.repetitions == 1) {
                result.intervalDays = 1;
            } else if (result.repetitions == 2) {
                result.intervalDays = 6;
            } else {
                result.intervalDays = (int) Math.round(intervalDays * newEase);
            }
            if (result.intervalDays > MAX_INTERVAL_DAYS) {
                result.intervalDays = MAX_INTERVAL_DAYS;
            }
        }
        return result;
    }

    /** 算完之后的新状态 */
    public static class Result {
        public double ease;
        public int intervalDays;
        public int repetitions;
        public int lapses;
    }
}
