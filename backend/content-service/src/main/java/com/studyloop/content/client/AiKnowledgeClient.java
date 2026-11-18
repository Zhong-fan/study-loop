package com.studyloop.content.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.studyloop.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ai-service", contextId = "aiKnowledgeClient", path = "/api/ai")
public interface AiKnowledgeClient {

    @PostMapping("/extract-knowledge")
    Result<List<JsonNode>> extract(@RequestHeader("X-User-Id") Long userId,
                                   @RequestBody Map<String, Object> body);
}
