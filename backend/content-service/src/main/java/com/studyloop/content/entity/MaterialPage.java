package com.studyloop.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料里的一页。
 * <p>
 * PDF 上传后先抽纯文本；解析流水线再把这一页交给大模型，
 * 产出结构化块、强调信号和原始题目，存到下面的 JSON 字段里。
 */
@Data
@TableName("material_page")
public class MaterialPage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long materialId;

    private Long userId;

    /** 从 1 开始，和阅读器显示的页码一致 */
    private Integer pageNo;

    /** 纯文本，解析前先用它兜底 */
    private String content;

    /** 结构化块 JSON（blocks） */
    private String blocks;

    /** 强调信号 JSON（emphasis） */
    private String emphasis;

    /** SLIDE / PDF / IMAGE / DOC */
    private String docType;

    /** PENDING / SUCCESS / NEEDS_REVIEW */
    private String parseStatus;

    /** 解析警告 JSON 数组 */
    private String parseWarnings;

    private LocalDateTime createdAt;
}
