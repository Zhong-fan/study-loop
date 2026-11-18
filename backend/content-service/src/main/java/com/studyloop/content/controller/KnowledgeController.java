package com.studyloop.content.controller;

import com.studyloop.common.context.UserContext;
import com.studyloop.common.result.Result;
import com.studyloop.content.dto.KnowledgeRequest;
import com.studyloop.content.service.KnowledgeExtractService;
import com.studyloop.content.service.KnowledgeService;
import com.studyloop.content.vo.KnowledgeVO;
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

import java.util.List;

@Tag(name = "知识点")
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeExtractService extractService;

    public KnowledgeController(KnowledgeService knowledgeService, KnowledgeExtractService extractService) {
        this.knowledgeService = knowledgeService;
        this.extractService = extractService;
    }

    @Operation(summary = "某章下的知识点")
    @GetMapping
    public Result<List<KnowledgeVO>> listByChapter(@RequestParam Long chapterId) {
        return Result.ok(knowledgeService.listByChapter(chapterId));
    }

    @Operation(summary = "某门课的全部知识点")
    @GetMapping("/by-course")
    public Result<List<KnowledgeVO>> listByCourse(@RequestParam Long courseId) {
        return Result.ok(knowledgeService.listByCourse(courseId));
    }

    @Operation(summary = "某门课的知识点总数（覆盖统计用）")
    @GetMapping("/count")
    public Result<Long> count(@RequestParam Long courseId) {
        return Result.ok(knowledgeService.countByCourse(courseId));
    }

    @Operation(summary = "新增知识点")
    @PostMapping("/{courseId}")
    public Result<Long> create(@PathVariable Long courseId, @RequestBody @Valid KnowledgeRequest request) {
        return Result.ok(knowledgeService.create(courseId, request));
    }

    @Operation(summary = "修改知识点")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid KnowledgeRequest request) {
        knowledgeService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除知识点")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "按证据算加权的知识点（预测卷用）")
    @GetMapping("/weighted")
    public Result<java.util.List<com.studyloop.content.vo.WeightedKnowledgeVO>> weighted(@RequestParam Long courseId) {
        return Result.ok(knowledgeService.weightedByCourse(courseId));
    }

    @Operation(summary = "某门课全部知识点的证据（前端展示出处用）")
    @GetMapping("/evidence")
    public Result<java.util.List<com.studyloop.content.vo.KnowledgeEvidenceVO>> evidence(@RequestParam Long courseId) {
        return Result.ok(knowledgeService.evidenceByCourse(courseId));
    }

    @Operation(summary = "从已解析资料自动抽取知识点（全自动，重复运行不会重复入库）")
    @PostMapping("/extract")
    public Result<java.util.Map<String, Object>> extract(@RequestParam Long courseId,
                                                         @RequestParam Long chapterId,
                                                         @RequestParam Long materialId) {
        Long userId = UserContext.requireUserId();
        return Result.ok(extractService.extract(userId, courseId, chapterId, materialId));
    }
}
