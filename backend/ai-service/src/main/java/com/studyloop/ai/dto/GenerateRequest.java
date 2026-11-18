package com.studyloop.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateRequest {

    private Long knowledgeId;

    @NotBlank(message = "知识点名称不能为空")
    private String knowledgeName;

    /** 生成几道，默认 3 */
    @Min(1)
    @Max(10)
    private Integer count = 3;

    /** 想要哪种题型，默认单选 */
    private String type = "SINGLE";
}
