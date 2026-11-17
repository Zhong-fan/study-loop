package com.studyloop.content.vo;

import lombok.Data;

/**
 * 返回给前端的章节结构。
 */
@Data
public class ChapterVO {

    private Long id;
    private Long courseId;
    private String title;
    private Integer sort;
}
