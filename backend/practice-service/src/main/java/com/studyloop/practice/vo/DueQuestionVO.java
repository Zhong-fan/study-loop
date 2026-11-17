package com.studyloop.practice.vo;

import lombok.Data;

import java.util.List;

/**
 * 今日待练的一道题（不含答案）。
 */
@Data
public class DueQuestionVO {

    private Long questionId;

    private Long knowledgeId;

    private String type;

    private String stem;

    private List<String> options;

    private Integer difficulty;
}
