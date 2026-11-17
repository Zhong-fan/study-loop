package com.studyloop.insight.vo;

import lombok.Data;

@Data
public class CoverageVO {

    /** 课程下知识点总数（来自内容服务） */
    private long totalKnowledge;

    /** 练过的知识点数 */
    private long practiced;

    /** 正确率 ≥ 0.8 且练过至少 2 次，算掌握 */
    private long mastered;

    /** mastered / totalKnowledge，0~1 */
    private double coverage;
}
