package com.studyloop.course.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 返回给前端的资料结构。
 * <p>
 * 注意没有 fileKey：存储路径属于内部实现，前端只需要 downloadUrl。
 */
@Data
public class MaterialVO {

    private Long id;
    private Long courseId;
    private Long chapterId;
    private String title;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String downloadUrl;
    private LocalDateTime createdAt;
}
