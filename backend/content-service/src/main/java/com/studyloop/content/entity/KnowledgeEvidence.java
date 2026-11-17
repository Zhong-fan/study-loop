package com.studyloop.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识点证据：一条知识点在原文里的出处。
 * <p>
 * 押题的可解释性全靠它——学生点开一个知识点，
 * 能看到它出现在第几页、原文怎么写的、是不是老师标了"必考"。
 */
@Data
@TableName("knowledge_evidence")
public class KnowledgeEvidence {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeId;

    private Long materialId;

    private Integer pageNo;

    private String quote;

    /** KEYWORD / VISUAL / REPEAT / EXAMPLE / OUTLINE */
    private String signalType;

    private Double weight;

    /** 0 未校验 1 已校验 */
    private Integer verified;

    private LocalDateTime createdAt;
}
