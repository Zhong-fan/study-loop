package com.studyloop.practice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 一次作答里，某个知识点的得分。
 * <p>
 * 有了它，"单选对了但大题没答出来"才能被看见：
 * 同一个知识点会有 SINGLE 和 COMPOSITE 两种情境的记录。
 */
@Data
@TableName("attempt_detail")
public class AttemptDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long attemptId;

    private Long knowledgeId;

    private String questionType;

    /** SINGLE 单点情境 / COMPOSITE 综合情境 */
    private String context;

    private Double weight;

    /** 该知识点在本题的得分率 0~1 */
    private Double score;
}
