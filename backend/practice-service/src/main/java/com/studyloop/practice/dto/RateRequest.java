package com.studyloop.practice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 自评（主观题 / 图片题 / 手动复习）。
 */
@Data
public class RateRequest {

    @NotNull(message = "请选择评分")
    @Min(value = 0, message = "评分只能是 0、1、2")
    @Max(value = 2, message = "评分只能是 0、1、2")
    private Integer rating;

    private Long costMs;
}
