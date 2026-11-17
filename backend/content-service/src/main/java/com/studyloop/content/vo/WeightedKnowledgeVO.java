package com.studyloop.content.vo;

import lombok.Data;

/**
 * 带权重的知识点：预测卷按它分配题量。
 * weight 由证据信号、出现次数、重要度合成，全部可解释。
 */
@Data
public class WeightedKnowledgeVO {

    private Long knowledgeId;
    private String name;
    private Integer importance;

    /** 综合权重 0~1 */
    private Double weight;

    /** 证据条数 */
    private Integer evidenceCount;

    /** 强调词命中次数（重点/必考/掌握） */
    private Integer keywordCount;

    /** 例题命中次数 */
    private Integer exampleCount;

    /** 出现过的页码，去重后拼接，便于展示"出自哪几页" */
    private String pages;
}
