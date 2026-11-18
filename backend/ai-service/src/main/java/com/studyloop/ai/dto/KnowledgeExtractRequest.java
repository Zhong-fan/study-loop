package com.studyloop.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgeExtractRequest {

    /** 资料 id，会原样写进证据，防止模型瞎编 */
    private Long materialId;

    @NotEmpty(message = "页面内容不能为空")
    private List<PageText> pages;

    @Data
    public static class PageText {
        private Integer page;
        private String text;
    }
}
