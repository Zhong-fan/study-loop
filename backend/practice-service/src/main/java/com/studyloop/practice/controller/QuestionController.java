package com.studyloop.practice.controller;

import com.studyloop.common.result.PageResult;
import com.studyloop.common.result.Result;
import com.studyloop.practice.dto.QuestionCreateRequest;
import com.studyloop.practice.dto.QuestionUpdateRequest;
import com.studyloop.practice.service.QuestionService;
import com.studyloop.practice.vo.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目接口。
 */
@Tag(name = "题目")
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @Operation(summary = "分页查询题目")
    @GetMapping
    public Result<PageResult<QuestionVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        return Result.ok(questionService.page(page, size, courseId, type, status));
    }

    @Operation(summary = "题目详情（含答案与解析）")
    @GetMapping("/{id}")
    public Result<QuestionVO> detail(@PathVariable Long id) {
        return Result.ok(questionService.get(id));
    }

    @Operation(summary = "新建题目（自动生成复习卡）")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid QuestionCreateRequest request) {
        return Result.ok(questionService.create(request));
    }

    @Operation(summary = "修改题目")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid QuestionUpdateRequest request) {
        questionService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除题目")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.ok();
    }
}
