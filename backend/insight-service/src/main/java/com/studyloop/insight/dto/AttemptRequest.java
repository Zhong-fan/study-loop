package com.studyloop.insight.dto;

import lombok.Data;

import java.util.List;

/**
 * 一次作答结果，由练习服务在判分后回调进来。
 * <p>
 * 新版本会带 details：一道题挂多个知识点时，每个知识点各自的得分。
 * 老版本只传 knowledgeId + correct，仍然兼容。
 */
@Data
public class AttemptRequest {

    /** 作答 id，用作幂等键；老调用方可以不传 */
    private Long attemptId;

    /** 主知识点，legacy 字段 */
    private Long knowledgeId;

    private Long courseId;

    /** legacy 字段：整题对错 */
    private Boolean correct;

    /** 按知识点拆开的作答明细 */
    private List<Detail> details;

    @Data
    public static class Detail {
        private Long knowledgeId;
        /** SINGLE 单点情境 / COMPOSITE 综合情境 */
        private String context;
        /** 该知识点在这道题上的得分率 0~1 */
        private Double score;
        private Double weight;
    }
}
