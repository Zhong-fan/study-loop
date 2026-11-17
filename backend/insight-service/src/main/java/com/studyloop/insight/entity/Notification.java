package com.studyloop.insight.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** DUE 今日待练 */
    private String type;

    private String title;

    private String content;

    /** 空表示未读 */
    private LocalDateTime readAt;

    /** 同一天同类只提醒一次 */
    private String dedupKey;

    private LocalDateTime createdAt;
}
