package com.studyloop.practice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 复习卡：一道题一张卡，记录"下次什么时候该复习"。
 * <p>
 * 三个核心变量来自 SM-2：难度系数 ease、间隔 interval、连续答对次数 repetitions。
 * 答错时 repetitions 归零、lapses 加一。
 */
@Data
@TableName("review_card")
public class ReviewCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    /** NEW 新卡 / REVIEW 复习中 */
    private String state;

    /** 难度系数，初始 2.5，范围 1.3 ~ 2.8 */
    private Double easeFactor;

    /** 当前间隔（天） */
    private Integer intervalDays;

    /** 连续答对次数 */
    private Integer repetitions;

    /** 累计答错次数 */
    private Integer lapses;

    /** 到期日：due_date <= 今天 就是要复习 */
    private LocalDate dueDate;

    private LocalDateTime lastReviewAt;

    private LocalDateTime createdAt;

    private Integer deleted;
}
