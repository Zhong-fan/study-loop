package com.studyloop.practice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

/**
 * 保存 AI 出的题目草稿。
 * 字段和 ai-service 的 question-draft 契约一一对应，
 * 前端原样透传即可，不用理解内部结构。
 * <p>
 * AI 返回的是 snake_case，前端也可能传 camelCase，两种都收。
 */
@Data
public class QuestionDraftRequest {

    private String type;
    private String stem;
    private List<String> options;
    private List<String> answer;

    @JsonAlias("total_score")
    private Double totalScore;

    private Integer difficulty;
    private String bloom;
    private String source;
    private List<KnowledgeRef> knowledge;

    @JsonAlias("sub_questions")
    private List<SubQuestion> subQuestions;

    private List<Evidence> evidence;

    @JsonAlias({"analysis", "explain"})
    private String explain;

    @Data
    public static class KnowledgeRef {

        @JsonAlias("knowledge_id")
        private Long knowledgeId;
        private Double weight;
        /** PRIMARY / RELATED / CONFUSABLE */
        private String role;
    }

    @Data
    public static class SubQuestion {
        private Integer no;
        private String stem;
        private Double score;
        private String answer;
        private List<Rubric> rubric;
    }

    @Data
    public static class Rubric {

        @JsonAlias({"point", "point_text"})
        private String point;
        private Double score;

        @JsonAlias("knowledge_id")
        private Long knowledgeId;
    }

    @Data
    public static class Evidence {

        @JsonAlias("material_id")
        private Long materialId;
        private Integer page;
        private String quote;
    }
}
