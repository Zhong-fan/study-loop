package com.studyloop.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 章节，挂在课程下面。
 * <p>
 * sort 是显示顺序，数字小的排前面；不靠 id 排序，
 * 因为用户可能中途插入一章，id 顺序和阅读顺序对不上。
 */
@Data
@TableName("chapter")
public class Chapter {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    /** 冗余一份 userId，查章节时不用每次都先查课程 */
    private Long userId;

    private String title;

    private Integer sort;

    private LocalDateTime createdAt;

    private Integer deleted;
}
