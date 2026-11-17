package com.studyloop.insight.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 知识点掌握度：一个用户在一个知识点上的练习记录。
 * <p>
 * 它替代了原来的 SM-2 复习卡：不再维护 ease / repetitions，
 * 而是按「1 / 3 / 7 / 14 天」四档阶梯排下次复习。
 */
@Data
@TableName("mastery")
public class Mastery {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long knowledgeId;

    private Integer totalCount;

    private Integer correctCount;

    private Double accuracy;

    /** 阶梯序号 0~3，对应 1/3/7/14 天 */
    private Integer stage;

    private LocalDate nextReview;

    private LocalDateTime updatedAt;
}
