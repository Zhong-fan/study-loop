package com.studyloop.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("real_exam")
public class RealExam {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private String title;

    private Integer examYear;

    private String school;

    private Long materialId;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Integer deleted;
}
