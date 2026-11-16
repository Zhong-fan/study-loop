package com.studyloop.course.controller;

import com.studyloop.common.result.Result;
import com.studyloop.course.dto.ChapterRequest;
import com.studyloop.course.service.ChapterService;
import com.studyloop.course.vo.ChapterVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 章节接口。
 */
@Tag(name = "章节")
@RestController
@RequestMapping("/api")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @Operation(summary = "查询某门课的章节列表")
    @GetMapping("/courses/{courseId}/chapters")
    public Result<List<ChapterVO>> list(@PathVariable Long courseId) {
        return Result.ok(chapterService.listByCourse(courseId));
    }

    @Operation(summary = "新增章节")
    @PostMapping("/courses/{courseId}/chapters")
    public Result<Long> create(@PathVariable Long courseId, @RequestBody @Valid ChapterRequest request) {
        return Result.ok(chapterService.create(courseId, request));
    }

    @Operation(summary = "修改章节")
    @PutMapping("/chapters/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid ChapterRequest request) {
        chapterService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除章节")
    @DeleteMapping("/chapters/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        chapterService.delete(id);
        return Result.ok();
    }
}
