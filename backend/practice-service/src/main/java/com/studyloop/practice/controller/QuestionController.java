package com.studyloop.practice.controller;

import com.studyloop.common.context.UserContext;
import com.studyloop.common.result.PageResult;
import com.studyloop.common.result.Result;
import com.studyloop.practice.dto.QuestionCreateRequest;
import com.studyloop.practice.dto.QuestionDraftRequest;
import com.studyloop.practice.dto.QuestionUpdateRequest;
import com.studyloop.practice.service.QuestionDraftService;
import com.studyloop.practice.service.QuestionExcelService;
import com.studyloop.practice.service.QuestionService;
import com.studyloop.practice.vo.ImportResultVO;
import com.studyloop.practice.vo.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 题目接口。
 */
@Tag(name = "题目")
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final QuestionDraftService draftService;
    private final QuestionExcelService excelService;

    public QuestionController(QuestionService questionService, QuestionDraftService draftService,
                              QuestionExcelService excelService) {
        this.questionService = questionService;
        this.draftService = draftService;
        this.excelService = excelService;
    }

    @Operation(summary = "把 AI 草稿存入题库（含知识点关联与评分点）")
    @PostMapping("/from-drafts")
    public Result<java.util.List<Long>> saveDrafts(@RequestParam Long courseId,
                                                   @RequestParam(required = false) Long chapterId,
                                                   @RequestBody java.util.List<QuestionDraftRequest> drafts) {
        return Result.ok(draftService.save(UserContext.requireUserId(), courseId, chapterId, drafts));
    }

    @Operation(summary = "Excel 导入题目（默认整批失败回滚，skipInvalid=true 时跳过错误行）")
    @PostMapping("/import/excel")
    public Result<ImportResultVO> importExcel(@RequestParam("file") MultipartFile file,
                                              @RequestParam Long courseId,
                                              @RequestParam(required = false) Long chapterId,
                                              @RequestParam(defaultValue = "false") boolean skipInvalid) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要导入的文件");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new IllegalArgumentException("仅支持 .xlsx / .xls 文件");
        }
        try {
            return Result.ok(excelService.importExcel(file.getInputStream(), courseId, chapterId, skipInvalid));
        } catch (IOException e) {
            throw new IllegalArgumentException("读取文件失败：" + e.getMessage());
        }
    }

    @Operation(summary = "下载题目导入模板")
    @GetMapping("/template/excel")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        prepareExcelResponse(response, "question-import-template.xlsx");
        excelService.writeTemplate(response.getOutputStream());
    }

    @Operation(summary = "导出题目（按当前筛选条件，最多 5000 条）")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) Long courseId,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) Integer status,
                       HttpServletResponse response) throws IOException {
        prepareExcelResponse(response, "questions.xlsx");
        excelService.exportExcel(courseId, type, status, response.getOutputStream());
    }

    private void prepareExcelResponse(HttpServletResponse response, String filename) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
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
