package com.studyloop.insight.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 调内容服务拿知识点总数，用于算覆盖率。
 * <p>
 * 跨服务只传 id 和计数，不共享实体。
 */
@FeignClient(name = "content-service")
public interface ContentClient {

    @GetMapping("/api/knowledge/count")
    long countKnowledge(@RequestParam("courseId") Long courseId,
                        @RequestHeader("X-User-Id") Long userId);
}
