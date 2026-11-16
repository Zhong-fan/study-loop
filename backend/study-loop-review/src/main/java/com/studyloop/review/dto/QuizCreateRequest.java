package com.studyloop.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 组卷请求。
 */
@Data
public class QuizCreateRequest {

    @NotNull(message = "请选择课程")
    private Long courseId;

    @NotNull(message = "请填写题量")
    @Min(value = 1, message = "至少 1 道题")
    @Max(value = 50, message = "最多 50 道题")
    private Integer count;

    private String title;
}
