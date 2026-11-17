package com.studyloop.practice.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 提交作答（客观题）。
 * <p>
 * costMs 用来算答题耗时，前端可以不传。
 */
@Data
public class AnswerSubmitRequest {

    @NotEmpty(message = "请先作答")
    private List<String> answer;

    private Long costMs;
}
