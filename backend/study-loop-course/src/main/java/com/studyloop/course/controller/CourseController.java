package com.studyloop.course.controller;

import com.studyloop.common.result.PageResult;
import com.studyloop.common.result.Result;
import com.studyloop.course.dto.CourseCreateRequest;
import com.studyloop.course.dto.CourseUpdateRequest;
import com.studyloop.course.service.CourseService;
import com.studyloop.course.vo.CourseVO;
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
 * 课程接口。
 * <p>
 * Controller 只做三件事：接参数、调 service、包 Result。
 * 不写业务判断，也不直接碰 mapper。
 */
@Tag(name = "课程")
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Operation(summary = "分页查询我的课程")
    @GetMapping
    public Result<PageResult<CourseVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(courseService.page(page, size, keyword));
    }

    @Operation(summary = "查询课程详情")
    @GetMapping("/{id}")
    public Result<CourseVO> detail(@PathVariable Long id) {
        return Result.ok(courseService.getById(id));
    }

    @Operation(summary = "新建课程")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid CourseCreateRequest request) {
        return Result.ok(courseService.create(request));
    }

    @Operation(summary = "修改课程")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid CourseUpdateRequest request) {
        courseService.update(id, request);
        return Result.ok();
    }

    @Operation(summary = "删除课程")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.ok();
    }
}
