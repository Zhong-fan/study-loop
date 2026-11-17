package com.studyloop.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GradeRequest {

    private Long knowledgeId;

    @NotBlank(message = "题干不能为空")
    private String stem;

    /** 参考答案或评分要点 */
    private String reference;

    @NotBlank(message = "作答内容不能为空")
    private String answer;
}
