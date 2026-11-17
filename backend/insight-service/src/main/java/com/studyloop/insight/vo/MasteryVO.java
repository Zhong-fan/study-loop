package com.studyloop.insight.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MasteryVO {

    private Long knowledgeId;
    private Integer totalCount;
    private Integer correctCount;
    private Double accuracy;
    private Integer stage;
    private LocalDate nextReview;
}
