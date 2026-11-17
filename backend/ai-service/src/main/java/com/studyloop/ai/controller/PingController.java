package com.studyloop.ai.controller;

import com.studyloop.common.context.UserContext;
import com.studyloop.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "教育 AI")
@RestController
@RequestMapping("/api/ai")
public class PingController {

    @Operation(summary = "连通性检查")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("ai-service is up, userId=" + UserContext.get());
    }
}
