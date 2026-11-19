package com.studyloop.insight.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 已处理的作答事件：用 attemptId 做幂等键，重试不会重复累加。
 */
@Data
@TableName("attempt_event")
public class AttemptEvent {

    @TableId
    private Long attemptId;

    private LocalDateTime appliedAt;
}
