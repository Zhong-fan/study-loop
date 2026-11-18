package com.studyloop.practice.vo;

import lombok.Data;

import java.util.List;

/**
 * 交卷结果。
 */
@Data
public class QuizResultVO {

    private Long quizId;

    /** 百分制 */
    private double score;

    private int total;

    private int correctCount;

    private List<Item> items;

    @Data
    public static class Item {
        private Long questionId;
        private String type;
        private String stem;
        private List<String> yourAnswer;
        private List<String> correctAnswer;
        private double score;
        private String analysis;
    }
}
