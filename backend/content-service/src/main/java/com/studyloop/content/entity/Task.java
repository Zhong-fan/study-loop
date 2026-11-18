package com.studyloop.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 解析任务。
 * <p>
 * 上传资料后立刻解析，所以任务从 RUNNING 直接到 SUCCESS/FAILED；
 * 留这张表是为了以后换成异步队列时，状态机不用改。
 */
@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long materialId;

    /** PARSE 解析 / OCR 识别 */
    private String type;

    /** PENDING / RUNNING / SUCCESS / FAILED */
    private String status;

    private Integer retryCount;

    private String errorMsg;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
