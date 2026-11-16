package com.studyloop.review.controller;

import com.studyloop.common.context.UserContext;
import com.studyloop.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 连通性检查。
 * <p>
 * 这个接口的作用：确认网关把 X-User-Id 传过来了。
 * 换别人接手 review 服务后，第一件事可以先点它。
 */
@Tag(name = "复习")
@RestController
@RequestMapping("/api/review")
public class PingController {

    @Operation(summary = "连通性检查")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("review service is up, userId=" + UserContext.get());
    }
}
