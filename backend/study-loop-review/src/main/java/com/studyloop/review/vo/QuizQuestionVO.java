package com.studyloop.review.vo;

import lombok.Data;

import java.util.List;

/**
 * 答题时看到的一道题：没有答案，没有解析。
 */
@Data
public class QuizQuestionVO {

    private Long questionId;
    private String type;
    private String stem;
    private List<String> options;
    private Integer difficulty;
    private Integer sort;
}
