package com.studyloop.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 复习流水，只追加不修改。
 * <p>
 * 统计（正确率、连续天数、薄弱章节）全部从这张表算出来，
 * 所以每次复习都必须写一条，否则统计数据会失真。
 */
@Data
@TableName("review_log")
public class ReviewLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long cardId;

    private Long userId;

    /** 0 忘记 / 1 模糊 / 2 记住 */
    private Integer rating;

    private Integer prevInterval;

    private Integer nextInterval;

    private Double prevEase;

    private Double nextEase;

    /** 答题耗时，毫秒；前端不传就是 0 */
    private Long costMs;

    private LocalDateTime reviewedAt;
}
