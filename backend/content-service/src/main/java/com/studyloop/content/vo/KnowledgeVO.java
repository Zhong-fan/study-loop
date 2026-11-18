package com.studyloop.content.vo;

import lombok.Data;

@Data
public class KnowledgeVO {

    private Long id;
    private Long courseId;
    private Long chapterId;
    private String name;
    private String type;
    private Integer importance;
    private Integer pageNo;
    private Long materialId;
}
