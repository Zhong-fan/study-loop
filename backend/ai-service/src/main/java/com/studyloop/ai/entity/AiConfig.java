package com.studyloop.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_config")
public class AiConfig {

    @TableId(type = IdType.INPUT)
    private Integer id;

    private Integer enabled;

    private String baseUrl;

    private String model;

    private String apiKeyEnc;

    private Long updatedBy;

    private LocalDateTime updatedAt;
}
