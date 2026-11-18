package com.studyloop.insight.service;

import com.studyloop.insight.entity.Notification;

import java.util.List;
import java.util.Map;

/**
 * 站内信。
 * <p>
 * 没有做定时任务扫描全表用户：insight 没有用户表，也不该去关心有哪些人。
 * 改成用户打开页面时按需生成——用户不看就没有提醒的意义。
 */
public interface NotificationService {

    /** 拉取消息，顺便按今天的情况补一条"今日待练" */
    Map<String, Object> list(Long userId);

    /** 全部标为已读 */
    void markAllRead(Long userId);

    List<Notification> latest(Long userId, int limit);
}
