package com.studyloop.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.studyloop.ai.dto.KnowledgeExtractRequest;

import java.util.List;

public interface KnowledgeExtractService {

    /** 从若干页原文里抽知识点候选，逐个过 Schema 校验 */
    List<JsonNode> extract(KnowledgeExtractRequest request);
}
