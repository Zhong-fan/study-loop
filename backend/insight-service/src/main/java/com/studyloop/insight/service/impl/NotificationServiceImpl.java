package com.studyloop.insight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.studyloop.insight.entity.Mastery;
import com.studyloop.insight.entity.Notification;
import com.studyloop.insight.mapper.MasteryMapper;
import com.studyloop.insight.mapper.NotificationMapper;
import com.studyloop.insight.service.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final MasteryMapper masteryMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper, MasteryMapper masteryMapper) {
        this.notificationMapper = notificationMapper;
        this.masteryMapper = masteryMapper;
    }

    @Override
    public Map<String, Object> list(Long userId) {
        refreshDue(userId);

        List<Notification> items = latest(userId, 20);
        long unread = items.stream().filter(n -> n.getReadAt() == null).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unread", unread);
        result.put("items", items);
        return result;
    }

    @Override
    public void markAllRead(Long userId) {
        UpdateWrapper<Notification> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.isNull("read_at");
        wrapper.set("read_at", LocalDateTime.now());
        notificationMapper.update(null, wrapper);
    }

    @Override
    public List<Notification> latest(Long userId, int limit) {
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("created_at");
        wrapper.last("limit " + Math.max(1, Math.min(limit, 50)));
        return notificationMapper.selectList(wrapper);
    }

    /** 今天有到期的知识点就写一条，同一天不会重复 */
    private void refreshDue(Long userId) {
        LocalDate today = LocalDate.now();

        QueryWrapper<Mastery> dueWrapper = new QueryWrapper<>();
        dueWrapper.eq("user_id", userId);
        dueWrapper.le("next_review", today);
        long due = masteryMapper.selectCount(dueWrapper);
        if (due == 0) {
            return;
        }

        String dedupKey = "DUE:" + today;
        QueryWrapper<Notification> existsWrapper = new QueryWrapper<>();
        existsWrapper.eq("user_id", userId);
        existsWrapper.eq("dedup_key", dedupKey);
        if (notificationMapper.selectCount(existsWrapper) > 0) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType("DUE");
        notification.setTitle("今日待练");
        notification.setContent("有 " + due + " 个知识点到期，记得复习");
        notification.setDedupKey(dedupKey);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);
    }
}
