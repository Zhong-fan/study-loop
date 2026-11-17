package com.studyloop.content.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeRequest {

    @NotNull(message = "必须指定所属章")
    private Long chapterId;

    @NotBlank(message = "知识点名称不能为空")
    private String name;

    private String type;

    @Min(value = 1, message = "重要度 1-5")
    @Max(value = 5, message = "重要度 1-5")
    private Integer importance;

    private Integer pageNo;

    private Long materialId;
}
