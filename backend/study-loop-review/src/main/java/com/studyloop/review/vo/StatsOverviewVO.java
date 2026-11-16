package com.studyloop.review.vo;

import lombok.Data;

/**
 * 统计首页的数字。
 */
@Data
public class StatsOverviewVO {

    /** 题目总数 */
    private long totalQuestions;

    /** 已掌握（间隔 ≥ 30 天） */
    private long masteredQuestions;

    /** 今天到期该复习的卡 */
    private long dueToday;

    /** 今天已经复习了几次 */
    private long reviewedToday;

    /** 连续复习天数 */
    private int streakDays;

    /** 累计复习次数 */
    private long totalReviews;
}
