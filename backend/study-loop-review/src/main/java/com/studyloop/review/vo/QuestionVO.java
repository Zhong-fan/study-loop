package com.studyloop.review.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目详情（编辑页用）。
 * <p>
 * 这里包含 answer 和 analysis，因为题目是用户自己录的；
 * 复习页不要用这个类，用 ReviewCardVO，否则答案会提前泄露。
 */
@Data
public class QuestionVO {

    private Long id;
    private Long courseId;
    private Long chapterId;
    private String type;
    private String stem;
    private List<String> options;
    private List<String> answer;
    private String analysis;
    private Integer difficulty;
    private Integer status;
    private LocalDateTime createdAt;
}
