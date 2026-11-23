package com.studyloop.ai.controller;

import com.studyloop.ai.config.AiConfigService;
import com.studyloop.ai.config.AiProperties;
import com.studyloop.ai.config.AiRuntimeConfig;
import com.studyloop.ai.dto.GenerateRequest;
import com.studyloop.ai.dto.GradeRequest;
import com.studyloop.ai.dto.PageParseRequest;
import com.studyloop.ai.service.AiService;
import com.studyloop.ai.service.PageParseService;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final PageParseService pageParseService;
    private final com.studyloop.ai.service.KnowledgeExtractService knowledgeExtractService;
    private final com.studyloop.ai.service.DraftGenerateService draftGenerateService;
    private final com.studyloop.ai.service.ExamExtractService examExtractService;
    private final AiConfigService aiConfigService;
    private final AiProperties properties;

    public AiController(AiService aiService, PageParseService pageParseService,
                        com.studyloop.ai.service.KnowledgeExtractService knowledgeExtractService,
                        com.studyloop.ai.service.DraftGenerateService draftGenerateService,
                        com.studyloop.ai.service.ExamExtractService examExtractService,
                        AiConfigService aiConfigService,
                        AiProperties properties) {
        this.aiService = aiService;
        this.pageParseService = pageParseService;
        this.knowledgeExtractService = knowledgeExtractService;
        this.draftGenerateService = draftGenerateService;
        this.examExtractService = examExtractService;
        this.aiConfigService = aiConfigService;
        this.properties = properties;
    }

    @Operation(summary = "AI 是否可用")
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        AiRuntimeConfig config = aiConfigService.current();
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("usable", config.usable());
        status.put("model", config.model());
        status.put("baseUrl", config.baseUrl());
        status.put("keySet", config.apiKey() != null && !config.apiKey().isBlank());
        status.put("source", config.fromDb() ? "数据库" : "环境变量");
        status.put("updatedAt", config.updatedAt());
        return Result.ok(status);
    }

    @Operation(summary = "页面解析：一段原文 → 结构化页面")
    @PostMapping("/parse-page")
    public Result<JsonNode> parsePage(@RequestBody @Valid PageParseRequest request) {
        return Result.ok(pageParseService.parse(request));
    }

    @Operation(summary = "知识点抽取：几页原文 → 带证据的知识点候选")
    @PostMapping("/extract-knowledge")
    public Result<List<JsonNode>> extractKnowledge(
            @RequestBody @Valid com.studyloop.ai.dto.KnowledgeExtractRequest request) {
        return Result.ok(knowledgeExtractService.extract(request));
    }

    @Operation(summary = "从试卷页面抽取真题")
    @PostMapping("/extract-exam-items")
    public Result<List<JsonNode>> extractExamItems(@RequestBody Map<String, Object> body) {
        return Result.ok(examExtractService.extract(body));
    }

    @Operation(summary = "按知识点生成题目草稿（严格 Schema + 业务校验）")
    @PostMapping("/generate-drafts")
    public Result<List<JsonNode>> generateDrafts(
            @RequestBody @Valid com.studyloop.ai.dto.DraftGenerateRequest request) {
        return Result.ok(draftGenerateService.generate(request));
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
