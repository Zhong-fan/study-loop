package com.studyloop.content.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.studyloop.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * 调 ai-service 做页面解析。
 * 真正的模型调用在 ai-service 里，content-service 不碰 API Key。
 */
@FeignClient(name = "ai-service", contextId = "aiParseClient", path = "/api/ai")
public interface AiParseClient {

    @PostMapping("/parse-page")
    Result<JsonNode> parsePage(@RequestHeader("X-User-Id") Long userId,
                               @RequestBody Map<String, Object> body);
}
