package com.studyloop.practice.vo;

import lombok.Data;

import java.util.List;

@Data
public class PredictItemVO {

    private Long questionId;
    private String type;
    private String stem;

    /** 这道题挂在哪些知识点上 */
    private List<Long> knowledgeIds;

    /** 这道题的最高权重，代表"可能考"的程度 */
    private Double weight;

    /** 出题依据，直接给前端展示 */
    private String reason;
}
