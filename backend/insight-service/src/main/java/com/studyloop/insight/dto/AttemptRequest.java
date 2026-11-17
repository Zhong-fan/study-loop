package com.studyloop.insight.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 一次作答结果，由练习服务在判分后回调进来。
 */
@Data
public class AttemptRequest {

    @NotNull(message = "knowledgeId 不能为空")
    private Long knowledgeId;

    /** 所属课程，覆盖率按课程过滤要用 */
    private Long courseId;

    @NotNull(message = "correct 不能为空")
    private Boolean correct;
}
