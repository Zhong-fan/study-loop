package com.studyloop.practice.controller;

import com.studyloop.common.result.Result;
import com.studyloop.practice.dto.AnswerSubmitRequest;
import com.studyloop.practice.dto.RateRequest;
import com.studyloop.practice.service.ReviewService;
import com.studyloop.practice.vo.AnswerResultVO;
import com.studyloop.practice.vo.DueQueueVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 练习接口。
 * <p>
 * 两条路径：客观题走 /answer（服务端判分），主观题走 /rate（自评）。
 * 排期由覆盖服务决定，这里只负责判分与流水。
 */
@Tag(name = "练习")
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "今日待练（按知识点排期，附带没有题目的知识点）")
    @GetMapping("/due-questions")
    public Result<DueQueueVO> dueQuestions(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(reviewService.dueQuestions(limit));
    }

    @Operation(summary = "按题目作答（客观题，自动判分）")
    @PostMapping("/questions/{questionId}/answer")
    public Result<AnswerResultVO> answerQuestion(@PathVariable Long questionId,
                                                 @RequestBody @Valid AnswerSubmitRequest request) {
        return Result.ok(reviewService.answerQuestion(questionId, request.getAnswer(), request.getCostMs()));
    }

    @Operation(summary = "按题目自评（主观题）")
    @PostMapping("/questions/{questionId}/rate")
    public Result<AnswerResultVO> rateQuestion(@PathVariable Long questionId,
                                               @RequestBody @Valid RateRequest request) {
        return Result.ok(reviewService.rateQuestion(questionId, request.getRating(), request.getCostMs()));
    }
}
