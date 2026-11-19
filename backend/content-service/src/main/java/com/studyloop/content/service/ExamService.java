package com.studyloop.content.service;

import com.studyloop.content.dto.ExamCreateRequest;

import java.util.List;
import java.util.Map;

public interface ExamService {

    Long create(Long userId, ExamCreateRequest request);

    List<Map<String, Object>> list(Long userId, Long courseId);

    void delete(Long userId, Long id);

    /** 从已解析的资料里抽题，写入这张试卷 */
    Map<String, Object> importItems(Long userId, Long examId, Long materialId);

    /** 知识点 / 章节分布，回答"这门课爱考什么" */
    Map<String, Object> distribution(Long userId, Long examId);

    /** 给预测模型用的真题频次 */
    List<Map<String, Object>> weighted(Long courseId);
}
