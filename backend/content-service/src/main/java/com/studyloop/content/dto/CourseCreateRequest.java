package com.studyloop.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建课程的请求体。
 * <p>
 * DTO 和 Entity 分开写，是为了防止前端多传一个 userId 就把课程挂到别人名下。
 */
@Data
public class CourseCreateRequest {

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 50, message = "课程名称不能超过 50 个字")
    private String name;

    @Size(max = 200, message = "课程简介不能超过 200 个字")
    private String description;

    private String color;
}
