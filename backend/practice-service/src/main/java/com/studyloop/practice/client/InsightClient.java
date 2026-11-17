package com.studyloop.practice.client;

import com.studyloop.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * 和覆盖服务交互：查今天该练哪些知识点、判分后回写结果。
 * <p>
 * 返回值统一用 Result 包着 —— 直接写裸类型会反序列化失败。
 */
@FeignClient(name = "insight-service")
public interface InsightClient {

    @PostMapping("/api/insight/attempts")
    Result<MasteryDue> record(@RequestHeader("X-User-Id") Long userId, @RequestBody AttemptPayload payload);

    @GetMapping("/api/insight/due")
    Result<List<MasteryDue>> due(@RequestHeader("X-User-Id") Long userId);
}
