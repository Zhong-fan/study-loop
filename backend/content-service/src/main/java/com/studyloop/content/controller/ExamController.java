package com.studyloop.content.controller;

import com.studyloop.common.context.UserContext;
import com.studyloop.common.result.Result;
import com.studyloop.content.dto.ExamCreateRequest;
import com.studyloop.content.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "真题")
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @Operation(summary = "我的试卷列表")
    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Long courseId) {
        return Result.ok(examService.list(UserContext.requireUserId(), courseId));
    }

    @Operation(summary = "新建试卷")
    @PostMapping
    public Result<Long> create(@RequestBody ExamCreateRequest request) {
        return Result.ok(examService.create(UserContext.requireUserId(), request));
    }

    @Operation(summary = "删除试卷")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        examService.delete(UserContext.requireUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "从资料抽题导入")
    @PostMapping("/{id}/import")
    public Result<Map<String, Object>> importItems(@PathVariable Long id, @RequestParam Long materialId) {
        return Result.ok(examService.importItems(UserContext.requireUserId(), id, materialId));
    }

    @Operation(summary = "知识点分布")
    @GetMapping("/{id}/distribution")
    public Result<Map<String, Object>> distribution(@PathVariable Long id) {
        return Result.ok(examService.distribution(UserContext.requireUserId(), id));
    }

    @Operation(summary = "真题频次（给预测模型用）")
    @GetMapping("/weighted")
    public Result<List<Map<String, Object>>> weighted(@RequestParam Long courseId) {
        return Result.ok(examService.weighted(courseId));
    }
}
