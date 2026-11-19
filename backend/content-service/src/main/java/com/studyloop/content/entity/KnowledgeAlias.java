package com.studyloop.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识点别名：抽取时模型给出的同义说法，用来识别重复。
 */
@Data
@TableName("knowledge_alias")
public class KnowledgeAlias {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeId;

    private String alias;

    private String aliasKey;

    private LocalDateTime createdAt;
}
