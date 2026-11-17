package com.studyloop.insight.controller;

import com.studyloop.common.context.UserContext;
import com.studyloop.common.result.Result;
import com.studyloop.insight.dto.AttemptRequest;
import com.studyloop.insight.service.MasteryService;
import com.studyloop.insight.vo.CoverageVO;
import com.studyloop.insight.vo.MasteryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "覆盖与调度")
@RestController
@RequestMapping("/api/insight")
public class MasteryController {

    private final MasteryService masteryService;

    public MasteryController(MasteryService masteryService) {
        this.masteryService = masteryService;
    }

    @Operation(summary = "记一次作答结果（练习服务判分后回调）")
    @PostMapping("/attempts")
    public Result<MasteryVO> record(@RequestBody @Valid AttemptRequest request) {
        Long userId = UserContext.requireUserId();
        return Result.ok(masteryService.record(userId, request.getKnowledgeId(),
                request.getCourseId(), request.getCorrect()));
    }

    @Operation(summary = "今日待练")
    @GetMapping("/due")
    public Result<List<MasteryVO>> due() {
        return Result.ok(masteryService.due(UserContext.requireUserId()));
    }

    @Operation(summary = "盲区清单（正确率低于 60%）")
    @GetMapping("/weak")
    public Result<List<MasteryVO>> weak() {
        return Result.ok(masteryService.weak(UserContext.requireUserId()));
    }

    @Operation(summary = "覆盖率")
    @GetMapping("/coverage")
    public Result<CoverageVO> coverage(@RequestParam Long courseId) {
        return Result.ok(masteryService.coverage(UserContext.requireUserId(), courseId));
    }

    @Operation(summary = "看板汇总：练过/掌握/今日到期")
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(masteryService.summary(UserContext.requireUserId()));
    }
}
