package com.studyloop.practice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 修改题目。
 * <p>
 * 不允许改 courseId：题目已经生成了复习卡，
 * 换课程会让"按课程统计"和卡片归属对不上。
 */
@Data
public class QuestionUpdateRequest {

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
