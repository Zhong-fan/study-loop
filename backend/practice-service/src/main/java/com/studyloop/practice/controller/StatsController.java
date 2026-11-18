package com.studyloop.practice.controller;

import com.studyloop.common.result.Result;
import com.studyloop.practice.service.StatsService;
import com.studyloop.practice.vo.StatsOverviewVO;
import com.studyloop.practice.vo.TrendPointVO;
import com.studyloop.practice.vo.WeakChapterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计接口。
 */
@Tag(name = "统计")
@RestController
@RequestMapping("/api/review/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @Operation(summary = "总览：题数、待复习、连续天数")
    @GetMapping("/overview")
    public Result<StatsOverviewVO> overview() {
        return Result.ok(statsService.overview());
    }

    @Operation(summary = "薄弱章节 Top 5")
    @GetMapping("/weak-chapters")
    public Result<List<WeakChapterVO>> weakChapters() {
        return Result.ok(statsService.weakChapters());
    }

    @Operation(summary = "复习趋势")
    @GetMapping("/trend")
    public Result<List<TrendPointVO>> trend(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(statsService.trend(days));
    }
}
