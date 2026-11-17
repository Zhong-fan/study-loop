package com.studyloop.practice.controller;

import com.studyloop.common.result.Result;
import com.studyloop.practice.dto.AnswerSubmitRequest;
import com.studyloop.practice.dto.RateRequest;
import com.studyloop.practice.service.ReviewService;
import com.studyloop.practice.vo.AnswerResultVO;
import com.studyloop.practice.vo.ReviewCardVO;
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
 * 复习接口。
 * <p>
 * 两条路径：客观题走 /answer（服务端判分），主观题走 /rate（自评）。
 */
@Tag(name = "复习")
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "今日待复习")
    @GetMapping("/today")
    public Result<List<ReviewCardVO>> today(@RequestParam(defaultValue = "50") int limit) {
        return Result.ok(reviewService.today(limit));
    }

    @Operation(summary = "提交作答（客观题，自动判分）")
    @PostMapping("/cards/{cardId}/answer")
    public Result<AnswerResultVO> answer(@PathVariable Long cardId,
                                         @RequestBody @Valid AnswerSubmitRequest request) {
        return Result.ok(reviewService.submitAnswer(cardId, request.getAnswer(), request.getCostMs()));
    }

    @Operation(summary = "自评（主观题 / 图片题）")
    @PostMapping("/cards/{cardId}/rate")
    public Result<AnswerResultVO> rate(@PathVariable Long cardId,
                                       @RequestBody @Valid RateRequest request) {
        return Result.ok(reviewService.rate(cardId, request.getRating(), request.getCostMs()));
    }
}
