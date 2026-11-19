package com.studyloop.practice.client;

import java.util.List;

/**
 * 回调覆盖服务时传的载荷。
 * <p>
 * 老字段（knowledgeId/correct）保留兼容；新字段 details 把一道题的得分
 * 按知识点拆开，让 insight 能分别记"单点情境"和"综合情境"。
 */
public record AttemptPayload(Long attemptId, Long knowledgeId, Long courseId, Boolean correct, List<Detail> details) {

    /** 兼容旧调用的构造器 */
    public AttemptPayload(Long knowledgeId, Long courseId, Boolean correct) {
        this(null, knowledgeId, courseId, correct, null);
    }

    public record Detail(Long knowledgeId, String context, Double score, Double weight) {
    }
}
