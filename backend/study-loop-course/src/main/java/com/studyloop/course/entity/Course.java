package com.studyloop.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程表对应的实体，字段和 course 表一一对应。
 * <p>
 * 注意：这里没有用 Lombok 的 @Builder、@Accessors 之类，
 * 就是为了让 get/set 一眼能看懂，排查问题时不用猜生成了什么方法。
 */
@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户，所有查询都必须带上它 */
    private Long userId;

    private String name;

    private String description;

    /** 前端用的颜色标记，比如 #409EFF */
    private String color;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 0 正常，1 已删除。不做物理删除，方便以后恢复 */
    private Integer deleted;
}
