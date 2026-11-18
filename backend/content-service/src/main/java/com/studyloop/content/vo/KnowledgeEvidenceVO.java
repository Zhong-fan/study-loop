package com.studyloop.content.vo;

import lombok.Data;

/**
 * 知识点证据：给前端展示"这个知识点为什么重要、出自哪一页"。
 */
@Data
public class KnowledgeEvidenceVO {

    private Long knowledgeId;
    private Long materialId;
    private Integer pageNo;
    private String quote;

    /** KEYWORD / VISUAL / REPEAT / EXAMPLE / OUTLINE */
    private String signalType;

    private Double weight;
}
