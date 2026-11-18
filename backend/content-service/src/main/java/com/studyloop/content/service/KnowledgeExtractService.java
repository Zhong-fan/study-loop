package com.studyloop.content.service;

import java.util.Map;

public interface KnowledgeExtractService {

    /** 从某份资料的已解析页里自动抽知识点，全部自动入库 */
    Map<String, Object> extract(Long userId, Long courseId, Long chapterId, Long materialId);
}
