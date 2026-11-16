package com.studyloop.review.vo;

import lombok.Data;

/**
 * 薄弱章节。
 * <p>
 * 这里只有 chapterId，没有章节名 —— 章节名在 course 服务的库里，
 * 跨库查不了。前端手上已经有章节列表，按 id 映射一下就行。
 */
@Data
public class WeakChapterVO {

    private Long chapterId;

    /** 评"忘记"的次数 */
    private long wrongCount;

    /** 该章节累计复习次数 */
    private long totalCount;

    /** 错误率，0~1 */
    private double wrongRate;
}
