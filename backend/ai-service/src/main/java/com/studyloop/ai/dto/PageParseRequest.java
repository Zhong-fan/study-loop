package com.studyloop.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PageParseRequest {

    @NotNull(message = "页码不能为空")
    @Min(1)
    private Integer page;

    /** SLIDE / PDF / IMAGE / DOC */
    private String docType = "PDF";

    /** 文字层内容；扫描件可以为空 */
    private String text;

    /** 可选：data:image/png;base64,... 扫描件靠它识别 */
    private String image;
}
