package com.studyloop.practice.client;

import lombok.Data;

import java.time.LocalDate;

/**
 * 覆盖服务返回的掌握度快照。
 * <p>
 * 练习服务只关心"下次什么时候复习"，其余字段备用，不参与判分。
 */
@Data
public class MasteryDue {

    private Long knowledgeId;

    private Integer totalCount;

    private Integer correctCount;

    private Double accuracy;

    private Integer stage;

    private LocalDate nextReview;
}
