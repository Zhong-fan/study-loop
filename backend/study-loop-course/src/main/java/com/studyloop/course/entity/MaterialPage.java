package com.studyloop.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资料里的一页文字。
 * <p>
 * PDF 上传后会被逐页抽成文本存到这里，
 * 有了它才能"搜索资料内容"和"从某一段直接出题"。
 * 目前只支持 PDF（扫描件抽不出文字，会是没有记录）。
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

    private String content;

    private LocalDateTime createdAt;
}
