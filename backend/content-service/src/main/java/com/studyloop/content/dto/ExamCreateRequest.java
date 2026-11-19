package com.studyloop.content.dto;

import lombok.Data;

@Data
public class ExamCreateRequest {

    private Long courseId;

    private String title;

    private Integer examYear;

    private String school;

    private Long materialId;
}
