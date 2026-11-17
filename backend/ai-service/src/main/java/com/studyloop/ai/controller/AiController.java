package com.studyloop.ai.controller;

import com.studyloop.ai.config.AiProperties;
import com.studyloop.ai.dto.GenerateRequest;
import com.studyloop.ai.dto.GradeRequest;
import com.studyloop.ai.service.AiService;
import com.studyloop.common.context.UserContext;
import com.studyloop.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "教育 AI")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final AiProperties properties;

    public AiController(AiService aiService, AiProperties properties) {
        this.aiService = aiService;
        this.properties = properties;
    }

    @Operation(summary = "AI 是否可用")
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.ok(Map.of(
                "usable", properties.usable(),
                "model", properties.getModel()
        ));
    }

    @Operation(summary = "按知识点生成题目草稿")
    @PostMapping("/questions")
    public Result<List<Map<String, Object>>> generate(@RequestBody @Valid GenerateRequest request) {
        return Result.ok(aiService.generateQuestions(UserContext.requireUserId(), request));
    }

    @Operation(summary = "主观题按要点判分")
    @PostMapping("/grade")
    public Result<Map<String, Object>> grade(@RequestBody @Valid GradeRequest request) {
        return Result.ok(aiService.gradeSubjective(UserContext.requireUserId(), request));
    }
}
