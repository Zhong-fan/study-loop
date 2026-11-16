package com.studyloop.review.dto;

import lombok.Data;

import java.util.List;

/**
 * 交卷请求：把每道题的作答一次性提交。
 */
@Data
public class QuizSubmitRequest {

    private List<Item> answers;

    @Data
    public static class Item {
        private Long questionId;
        private List<String> answer;
    }
}
