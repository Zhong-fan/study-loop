package com.studyloop.practice.client;

import com.studyloop.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 真题频次：哪个知识点在历年卷子里出现过几次。
 */
@FeignClient(name = "content-service", contextId = "contentExamClient", path = "/api/exams")
public interface ContentExamClient {

    @GetMapping("/weighted")
    Result<List<Map<String, Object>>> weighted(@RequestHeader("X-User-Id") Long userId,
                                               @RequestParam Long courseId);
}
