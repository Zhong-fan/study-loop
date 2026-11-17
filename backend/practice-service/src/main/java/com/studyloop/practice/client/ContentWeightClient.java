package com.studyloop.practice.client;

import com.studyloop.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 取内容服务算好的知识点权重，用来组预测卷。
 */
@FeignClient(name = "content-service", contextId = "contentWeightClient", path = "/api/knowledge")
public interface ContentWeightClient {

    @GetMapping("/weighted")
    Result<List<Map<String, Object>>> weighted(@RequestHeader("X-User-Id") Long userId,
                                               @RequestParam Long courseId);
}
