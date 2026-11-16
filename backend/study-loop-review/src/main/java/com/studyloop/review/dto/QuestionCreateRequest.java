package com.studyloop.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新建题目。
 * <p>
 * options 里只放选项内容，不加 "A." 前缀，序号由前端按下标渲染；
 * answer 存字母（"A"、"C"），和 options 的下标对应。
 */
@Data
public class QuestionCreateRequest {

    @NotNull(message = "必须选择课程")
    private Long courseId;

    private Long chapterId;

    @NotBlank(message = "题型不能为空")
    private String type;

    @NotBlank(message = "题干不能为空")
    private String stem;

    @Size(max = 6, message = "最多 6 个选项")
    private List<String> options;

    @NotEmpty(message = "答案不能为空")
    private List<String> answer;

    private String analysis;

    @Min(value = 1, message = "难度范围 1-5")
    @Max(value = 5, message = "难度范围 1-5")
    private Integer difficulty;
}
