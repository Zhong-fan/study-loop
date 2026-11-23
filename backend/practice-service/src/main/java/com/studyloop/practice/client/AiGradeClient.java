package com.studyloop.practice.client;

import com.studyloop.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "ai-service", contextId = "aiGradeClient", path = "/api/ai")
public interface AiGradeClient {

    @PostMapping("/grade")
    Result<Map<String, Object>> grade(@RequestHeader("X-User-Id") Long userId,
                                      @RequestBody Map<String, Object> body);
}
