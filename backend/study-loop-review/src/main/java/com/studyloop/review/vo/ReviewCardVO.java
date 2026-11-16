package com.studyloop.review.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 复习页的一张卡：卡片状态 + 题目内容。
 * <p>
 * 故意不包含 answer 和 analysis —— 没作答之前，答案不能出现在网络响应里。
 */
@Data
public class ReviewCardVO {

    private Long cardId;
    private Long questionId;
    private String type;
    private String stem;
    private List<String> options;
    private Integer difficulty;

    private String state;
    private Integer intervalDays;
    private Integer repetitions;
    private LocalDate dueDate;
}
