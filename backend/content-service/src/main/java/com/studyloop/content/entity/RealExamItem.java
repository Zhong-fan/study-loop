package com.studyloop.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("real_exam_item")
public class RealExamItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Integer seq;

    private String itemType;

    private String stem;

    private String answer;

    private BigDecimal score;

    private Integer pageNo;

    /** 题干归一化键，导入去重用 */
    private String stemKey;

    private Integer deleted;

    private Long knowledgeId;

    private Long chapterId;

    private LocalDateTime createdAt;
}
