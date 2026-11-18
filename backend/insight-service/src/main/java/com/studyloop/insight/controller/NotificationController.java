package com.studyloop.insight.controller;

import com.studyloop.common.context.UserContext;
import com.studyloop.common.result.Result;
import com.studyloop.insight.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "站内信")
@RestController
@RequestMapping("/api/insight/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "消息列表（顺便生成今日待练提醒）")
    @GetMapping
    public Result<Map<String, Object>> list() {
        return Result.ok(notificationService.list(UserContext.requireUserId()));
    }

    @Operation(summary = "全部标为已读")
    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(UserContext.requireUserId());
        return Result.ok();
    }
}
