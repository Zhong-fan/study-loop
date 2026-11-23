package com.studyloop.ai.controller;

import com.studyloop.ai.client.LlmClient;
import com.studyloop.ai.client.UserAdminClient;
import com.studyloop.ai.config.AiConfigRequest;
import com.studyloop.ai.config.AiConfigService;
import com.studyloop.ai.config.AiRuntimeConfig;
import com.studyloop.common.context.UserContext;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "AI 配置（管理员）")
@RestController
@RequestMapping("/api/ai/admin/config")
public class AiConfigController {

    private final AiConfigService configService;
    private final UserAdminClient userAdminClient;
    private final LlmClient llmClient;

    public AiConfigController(AiConfigService configService, UserAdminClient userAdminClient,
                              LlmClient llmClient) {
        this.configService = configService;
        this.userAdminClient = userAdminClient;
        this.llmClient = llmClient;
    }

    @Operation(summary = "读取当前 AI 配置（key 脱敏）")
    @GetMapping
    public Result<Map<String, Object>> get() {
        requireAdmin();
        return Result.ok(view(configService.current()));
    }

    @Operation(summary = "保存 AI 配置；apiKey 留空表示不修改")
    @PutMapping
    public Result<Map<String, Object>> save(@RequestBody AiConfigRequest request) {
        Long userId = requireAdmin();
        return Result.ok(view(configService.save(request, userId)));
    }

    @Operation(summary = "测试连接（不落库）")
    @PostMapping("/test")
    public Result<Map<String, Object>> test(@RequestBody(required = false) AiConfigRequest request) {
        requireAdmin();
        AiRuntimeConfig config = configService.preview(request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("model", config.model());
        result.put("baseUrl", config.baseUrl());
        try {
            long cost = llmClient.ping(config.baseUrl(), config.model(), config.apiKey());
            result.put("ok", true);
            result.put("latencyMs", cost);
        } catch (Exception e) {
            result.put("ok", false);
            result.put("message", e.getMessage() == null ? "连接失败" : e.getMessage());
        }
        return Result.ok(result);
    }

    private Long requireAdmin() {
        Long userId = UserContext.requireUserId();
        Result<Boolean> response = userAdminClient.isAdmin(userId, userId);
        if (response == null || response.getCode() != 0 || !Boolean.TRUE.equals(response.getData())) {
            throw new BizException(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
        return userId;
    }

    private Map<String, Object> view(AiRuntimeConfig config) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("enabled", config.enabled());
        view.put("baseUrl", config.baseUrl());
        view.put("model", config.model());
        view.put("keySet", config.apiKey() != null && !config.apiKey().isBlank());
        view.put("source", config.fromDb() ? "数据库" : "环境变量");
        view.put("updatedAt", config.updatedAt());
        view.put("updatedBy", config.updatedBy());
        return view;
    }
}
