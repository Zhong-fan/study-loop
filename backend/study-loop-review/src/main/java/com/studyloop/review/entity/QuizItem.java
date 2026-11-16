package com.studyloop.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 卷子里的一道题（只存题目 id 和顺序）。
 */
@Data
@TableName("quiz_item")
public class QuizItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long quizId;

    private Long questionId;

    private Integer sort;
}
