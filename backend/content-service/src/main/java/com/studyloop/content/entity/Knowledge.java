package com.studyloop.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识点（节）。
 * <p>
 * 章下面是节，节就是一个知识点，是复习与统计的最小单位。
 * 页码指回资料，复习时可以跳回原文。
 */
@Data
@TableName("knowledge")
public class Knowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long courseId;

    /** 所属章 */
    private Long chapterId;

    private String name;

    /** CONCEPT 概念 / SKILL 方法 / CASE 案例 */
    private String type;

    /** 重要度 1-5，用来给试卷加权 */
    private Integer importance;

    private Integer pageNo;

    private Long materialId;

    private LocalDateTime createdAt;

    private Integer deleted;
}
