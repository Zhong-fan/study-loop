package com.studyloop.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class DraftGenerateRequest {

    /** SINGLE / MULTIPLE / JUDGE / BLANK / SUBJECTIVE / COMPOSITE */
    private String type = "SINGLE";

    @Min(1)
    @Max(5)
    private Integer count = 1;

    private Long materialId;

    @NotEmpty(message = "至少给一个知识点")
    private List<KnowledgeRef> knowledge;

    /** 可选：知识点在原文里的证据，用来让题目有依据 */
    private List<EvidenceRef> evidence;

    @Data
    public static class KnowledgeRef {
        private Long id;
        private String name;
    }

    @Data
    public static class EvidenceRef {
        private Long knowledgeId;
        private Integer page;
        private String quote;
        private String signal;
    }
}
