package com.studyloop.course.controller;

import com.studyloop.common.result.Result;
import com.studyloop.course.service.MaterialPageService;
import com.studyloop.course.service.MaterialService;
import com.studyloop.course.vo.DownloadFile;
import com.studyloop.course.vo.MaterialPageVO;
import com.studyloop.course.vo.MaterialVO;
import com.studyloop.course.vo.PageSearchHitVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 资料接口。
 * <p>
 * 上传用 multipart/form-data，下载直接返回文件流，
 * 这两个接口不能套 Result，否则前端拿到的是 JSON 而不是文件。
 */
@Tag(name = "资料")
@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService materialService;
    private final MaterialPageService pageService;

    public MaterialController(MaterialService materialService, MaterialPageService pageService) {
        this.materialService = materialService;
        this.pageService = pageService;
    }

    @Operation(summary = "上传资料")
    @PostMapping
    public Result<Long> upload(@RequestParam Long courseId,
                               @RequestParam(required = false) Long chapterId,
                               @RequestParam(required = false) String title,
                               @RequestParam("file") MultipartFile file) {
        return Result.ok(materialService.upload(courseId, chapterId, title, file));
    }

    @Operation(summary = "查询某门课的资料列表")
    @GetMapping
    public Result<List<MaterialVO>> list(@RequestParam Long courseId) {
        return Result.ok(materialService.listByCourse(courseId));
    }

    @Operation(summary = "删除资料")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return Result.ok();
    }

    @Operation(summary = "下载资料")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        DownloadFile file = materialService.download(id);

        // 中文文件名要编码，否则浏览器下载下来是乱码
        String encoded = URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentLength(file.size())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(file.stream()));
    }

    @Operation(summary = "查看资料逐页文字（PDF 解析结果）")
    @GetMapping("/{id}/pages")
    public Result<List<MaterialPageVO>> pages(@PathVariable Long id) {
        return Result.ok(pageService.listByMaterial(id));
    }

    @Operation(summary = "在资料文字里搜关键词")
    @GetMapping("/{id}/search")
    public Result<List<PageSearchHitVO>> search(@PathVariable Long id, @RequestParam String keyword) {
        return Result.ok(pageService.search(id, keyword));
    }
}
