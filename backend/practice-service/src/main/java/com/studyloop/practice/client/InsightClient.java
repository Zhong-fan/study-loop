package com.studyloop.practice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 判分后通知覆盖服务更新掌握度与下次复习时间。
 * <p>
 * 调用失败只记日志，不影响作答本身：练习已经完成了，统计晚一点补上没关系。
 */
@FeignClient(name = "insight-service")
public interface InsightClient {

    @PostMapping("/api/insight/attempts")
    void record(@RequestHeader("X-User-Id") Long userId, @RequestBody AttemptPayload payload);
}
