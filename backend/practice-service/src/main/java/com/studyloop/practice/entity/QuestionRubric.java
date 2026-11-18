package com.studyloop.practice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 评分点：综合题按点给分，每个点绑一个知识点。
 */
@Data
@TableName("question_rubric")
public class QuestionRubric {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    /** 综合题的小题号，单选/判断这类题为空 */
    private Integer subNo;

    private String pointText;

    private Double score;

    private Long knowledgeId;
}
