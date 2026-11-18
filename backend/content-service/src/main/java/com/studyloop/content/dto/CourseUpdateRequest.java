package com.studyloop.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改课程的请求体。
 * <p>
 * 和新建分开，是因为以后可能允许改名字但不允许改归属用户，
 * 两个场景的校验规则不一样，共用一个类迟早会互相牵制。
 */
@Data
public class CourseUpdateRequest {

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 50, message = "课程名称不能超过 50 个字")
    private String name;

    @Size(max = 200, message = "课程简介不能超过 200 个字")
    private String description;

    private String color;
}
