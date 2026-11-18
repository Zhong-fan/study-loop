package com.studyloop.practice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 一份自测卷。
 * <p>
 * 组卷只抽客观题：主观题没法自动判分，混进来会让"交卷即出分"这件事不成立。
 */
@Data
@TableName("quiz")
public class Quiz {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long courseId;

    private String title;

    private Integer questionCount;

    /** 0 进行中，1 已交卷 */
    private Integer status;

    /** 百分制得分，交卷后才有 */
    private Double score;

    private LocalDateTime createdAt;

    private LocalDateTime submittedAt;
}
