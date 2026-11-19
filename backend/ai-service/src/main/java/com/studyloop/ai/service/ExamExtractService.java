package com.studyloop.ai.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public interface ExamExtractService {

    /**
     * 从试卷页面里抽出题目条目。
     * 入参：{pages:[{page,text}]}
     */
    List<JsonNode> extract(Map<String, Object> body);
}
