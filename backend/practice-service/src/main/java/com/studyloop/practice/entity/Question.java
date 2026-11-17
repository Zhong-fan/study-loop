package com.studyloop.practice.entity;

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

    /** 归属知识点（节），统计和排期都以它为单位 */
    private Long knowledgeId;

    /** 题目来自哪份资料（可选）。存 id 而不是标题，资料改名不影响 */
    private Long materialId;

    /** 来自第几页，和资料解析出的页码一致 */
    private Integer pageNo;

    /** 出题时选中的那段原文，复习时用来"回看原文" */
    private String quote;

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
