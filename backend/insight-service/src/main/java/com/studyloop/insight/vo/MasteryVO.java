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

    /** 单点题正确率 */
    private Double surfaceAccuracy;

    /** 综合题得分率 */
    private Double deepAccuracy;

    /** 1 表示"会背不会用" */
    private Integer transferFlag;
}
