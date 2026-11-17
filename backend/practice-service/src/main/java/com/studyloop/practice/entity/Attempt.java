package com.studyloop.practice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一次作答记录。
 * <p>
 * 和 review_log 的区别：这条是「业务流水」，谁在什么时候对哪道题得了多少分、由谁判定；
 * 掌握度和覆盖率由 insight 服务根据它累计出来。
 */
@Data
@TableName("attempt")
public class Attempt {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    /** 题目挂的知识点，为空时不计入覆盖 */
    private Long knowledgeId;

    /** 0~1 */
    private Double score;

    /** AUTO 客观题自动 / SELF 自评 / AI 大模型判 */
    private String grader;

    private Long costMs;

    private LocalDateTime answeredAt;
}
