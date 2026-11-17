package com.studyloop.practice.controller;

import com.studyloop.common.result.Result;
import com.studyloop.practice.dto.QuizCreateRequest;
import com.studyloop.practice.dto.QuizSubmitRequest;
import com.studyloop.practice.service.QuizService;
import com.studyloop.practice.vo.QuizDetailVO;
import com.studyloop.practice.vo.QuizResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 组卷接口。
 */
@Tag(name = "自测")
@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @Operation(summary = "按课程随机组卷")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid QuizCreateRequest request) {
        return Result.ok(quizService.create(request));
    }

    @Operation(summary = "查看试卷（不含答案）")
    @GetMapping("/{id}")
    public Result<QuizDetailVO> detail(@PathVariable Long id) {
        return Result.ok(quizService.detail(id));
    }

    @Operation(summary = "交卷判分")
    @PostMapping("/{id}/submit")
    public Result<QuizResultVO> submit(@PathVariable Long id, @RequestBody QuizSubmitRequest request) {
        return Result.ok(quizService.submit(id, request));
    }
}
