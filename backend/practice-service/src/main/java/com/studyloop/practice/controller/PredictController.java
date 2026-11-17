package com.studyloop.practice.controller;

import com.studyloop.common.context.UserContext;
import com.studyloop.common.result.Result;
import com.studyloop.practice.service.PredictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "预测卷")
@RestController
@RequestMapping("/api/review")
public class PredictController {

    private final PredictService predictService;

    public PredictController(PredictService predictService) {
        this.predictService = predictService;
    }

    @Operation(summary = "按知识点权重组一份预测卷（每题带依据）")
    @GetMapping("/predict")
    public Result<Map<String, Object>> predict(@RequestParam Long courseId,
                                               @RequestParam(defaultValue = "10") int size) {
        return Result.ok(predictService.build(UserContext.requireUserId(), courseId, size));
    }
}
