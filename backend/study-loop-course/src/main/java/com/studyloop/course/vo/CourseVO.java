package com.studyloop.course.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 返回给前端的课程结构。
 * <p>
 * 只放前端需要的字段：userId、deleted 这类内部字段不出现在这里。
 */
@Data
public class CourseVO {

    private Long id;
    private String name;
    private String description;
    private String color;
    private LocalDateTime createdAt;
}
