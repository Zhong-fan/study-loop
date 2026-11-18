package com.studyloop.practice.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 今日待练队列。
 * <p>
 * questions 是能直接做的题；pendingKnowledgeIds 是到期但还没有题目的知识点，
 * 前端提示"去 AI 出题"，避免用户以为系统坏了。
 */
@Data
public class DueQueueVO {

    private List<DueQuestionVO> questions = new ArrayList<>();

    private List<Long> pendingKnowledgeIds = new ArrayList<>();
}
