package com.studyloop.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_log")
public class AiLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** GENERATE_QUESTION / GRADE_SUBJECTIVE / KEY_POINTS */
    private String scene;

    private Long knowledgeId;

    private String model;

    private Long costMs;

    private Integer success;

    private String errorMsg;

    private LocalDateTime createdAt;
}
