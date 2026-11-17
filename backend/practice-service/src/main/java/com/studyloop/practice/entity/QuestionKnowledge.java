package com.studyloop.practice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 一道题挂哪些知识点，以及各自的分量。
 * <p>
 * role 区分主考点、关联考点、易混点，掌握度计算和讲评都靠它。
 */
@Data
@TableName("question_knowledge")
public class QuestionKnowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private Long knowledgeId;

    /** PRIMARY / RELATED / CONFUSABLE */
    private String role;

    private Double weight;
}
