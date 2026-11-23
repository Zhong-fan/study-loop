package com.studyloop.ai.service;

import com.studyloop.ai.dto.GradeRequest;

import java.util.Map;

public interface AiService {

    /** 主观题判分，返回 score(0~1) 和 comment */
    Map<String, Object> gradeSubjective(Long userId, GradeRequest request);
}
