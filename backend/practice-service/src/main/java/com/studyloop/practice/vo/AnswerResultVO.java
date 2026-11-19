package com.studyloop.practice.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 判分结果。
 * <p>
 * 提交作答后才返回标准答案和解析，前端这时候才能显示。
 */
@Data
public class AnswerResultVO {

    /** 1.0 全对，0.5 部分对，0 错 */
    private double score;

    /** 由得分换算出的评分：0 忘记 / 1 模糊 / 2 记住 */
    private int rating;

    /** 下次复习日期，前端用它显示"明天再来" */
    private LocalDate nextDueDate;

    /** 掌握度是否已同步；false 表示稍后自动重试 */
    private Boolean masterySynced;

    private List<String> correctAnswer;

    private String analysis;
}
