package com.studyloop.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PageParseRequest {

    @NotNull(message = "页码不能为空")
    @Min(1)
    private Integer page;

    /** SLIDE / PDF / IMAGE / DOC */
    private String docType = "PDF";

    @NotBlank(message = "页面文字不能为空")
    private String text;
}
