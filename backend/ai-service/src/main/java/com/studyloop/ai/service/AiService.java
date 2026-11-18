package com.studyloop.ai.service;

import com.studyloop.ai.dto.GenerateRequest;
import com.studyloop.ai.dto.GradeRequest;

import java.util.List;
import java.util.Map;

public interface AiService {

    /** 生成题目草稿，返回给内容服务做人工确认 */
    List<Map<String, Object>> generateQuestions(Long userId, GenerateRequest request);

    /** 主观题判分，返回 score(0~1) 和 comment */
    Map<String, Object> gradeSubjective(Long userId, GradeRequest request);
}
