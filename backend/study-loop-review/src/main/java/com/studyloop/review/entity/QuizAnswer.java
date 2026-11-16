package com.studyloop.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一道题的作答记录。
 * <p>
 * 存 JSON 字符串而不是关联表，理由和题目选项一样：
 * 这些数据只整条读写，不做按内容查询。
 */
@Data
@TableName("quiz_answer")
public class QuizAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long quizId;

    private Long questionId;

    private String userAnswer;

    /** 1.0 全对 / 0.5 部分对 / 0 错 */
    private Double score;

    private Integer correct;

    private LocalDateTime createdAt;
}
