package com.studyloop.practice.vo;

import lombok.Data;

import java.util.List;

/**
 * 试卷详情。
 */
@Data
public class QuizDetailVO {

    private Long id;
    private String title;
    private Integer status;
    private List<QuizQuestionVO> questions;
}
