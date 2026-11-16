package com.studyloop.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目（错题）。
 * <p>
 * options 和 answer 存的是 JSON 字符串，不是关联表：
 * 题型有六种，拆成选项表会多出两张表、多一次 join，
 * 而这里从来不需要"按选项内容做 SQL 查询"。
 */
@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long courseId;

    private Long chapterId;

    /** SINGLE 单选 / MULTIPLE 多选 / JUDGE 判断 / BLANK 填空 / SUBJECTIVE 主观 / IMAGE 图片 */
    private String type;

    private String stem;

    /** JSON 数组，如 ["A. 红黑树", "B. 二叉堆"]；判断题和主观题为空 */
    private String options;

    /** JSON 数组，如 ["A"] 或 ["A","C"]；填空题是多个可接受答案 */
    private String answer;

    private String analysis;

    private Integer difficulty;

    /** 0 待复习，1 已掌握 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer deleted;
}
