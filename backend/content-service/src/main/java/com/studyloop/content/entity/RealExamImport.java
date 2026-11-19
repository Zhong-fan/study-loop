package com.studyloop.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 真题导入记录：同一份资料对同一张试卷只导一次。
 */
@Data
@TableName("real_exam_import")
public class RealExamImport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long materialId;

    private Integer itemCount;

    private LocalDateTime createdAt;
}
