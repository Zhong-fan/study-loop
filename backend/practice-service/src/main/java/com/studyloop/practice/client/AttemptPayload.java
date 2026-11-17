package com.studyloop.practice.client;

/**
 * 回调覆盖服务时传的最小载荷。
 * <p>
 * 只传知识点 id 和对错，正确率的累计在 insight 那边做，避免两边各算一套。
 */
public record AttemptPayload(Long knowledgeId, Boolean correct) {
}
