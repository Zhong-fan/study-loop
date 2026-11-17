package com.studyloop.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.studyloop.ai.dto.DraftGenerateRequest;

import java.util.List;

public interface DraftGenerateService {

    /** 按题型模板生成题目草稿，输出严格符合 question-draft Schema */
    List<JsonNode> generate(DraftGenerateRequest request);
}
