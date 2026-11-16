package com.studyloop.review.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 趋势图上的一个点。
 */
@Data
public class TrendPointVO {

    private LocalDate statDate;

    private long reviewCount;

    private long correctCount;

    /** 正确率，0~1 */
    private double accuracy;
}
