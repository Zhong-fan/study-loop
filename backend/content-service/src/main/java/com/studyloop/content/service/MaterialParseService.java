package com.studyloop.content.service;

import java.util.Map;

public interface MaterialParseService {

    /**
     * 把某份资料还没解析的页逐页交给大模型，写回结构化结果。
     *
     * @return total / parsed / needsReview 三个计数
     */
    Map<String, Object> parseMaterial(Long userId, Long materialId);
}
