package com.studyloop.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studyloop.ai.client.LlmClient;
import com.studyloop.ai.config.AiProperties;
import com.studyloop.ai.dto.DraftGenerateRequest;
import com.studyloop.ai.service.DraftGenerateService;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.validate.JsonSchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 出题：按题型模板生成，不让模型自由发挥。
 * <p>
 * 硬约束（代码校验，不靠 prompt）：
 * 1. 输出必须过 question-draft Schema；
 * 2. 引用的 knowledge_id 必须在给定列表里；
 * 3. 小题分值之和 = 总分，评分点分值之和 = 对应小题分值；
 * 4. 综合题要有主考点、关联考点、易混点。
 */
@Service
public class DraftGenerateServiceImpl implements DraftGenerateService {

    private static final Logger LOG = LoggerFactory.getLogger(DraftGenerateServiceImpl.class);

    private static final String SYSTEM_PROMPT = """
            你是一位命题老师。你只输出 JSON 数组，不要解释，不要 markdown 代码块。
            每一道题都必须挂到给定的知识点上，不能引用列表外的知识点。
            """;

    private final LlmClient llmClient;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public DraftGenerateServiceImpl(LlmClient llmClient, AiProperties properties, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<JsonNode> generate(DraftGenerateRequest request) {
        if (!properties.usable()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "AI 未配置，无法出题");
        }
        if (request.getKnowledge() == null || request.getKnowledge().isEmpty()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "至少需要一个知识点");
        }

        Set<Long> allowedIds = request.getKnowledge().stream()
                .map(DraftGenerateRequest.KnowledgeRef::getId)
                .collect(Collectors.toSet());

        List<String> errors = List.of();
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String content = llmClient.chat(SYSTEM_PROMPT, buildPrompt(request) + retryHint(errors));
                JsonNode root = objectMapper.readTree(stripFence(content));
                if (!root.isArray()) {
                    errors = List.of("根节点必须是数组");
                    continue;
                }

                List<JsonNode> drafts = new ArrayList<>();
                List<String> problems = new ArrayList<>();
                for (JsonNode item : root) {
                    if (!(item instanceof ObjectNode node)) {
                        problems.add("元素不是对象");
                        continue;
                    }
                    normalize(node, request.getMaterialId());
                    List<String> itemErrors = validateDraft(node, allowedIds, request.getType(), request.getEvidence());
                    if (itemErrors.isEmpty()) {
                        drafts.add(node);
                    } else {
                        problems.add("第 " + (drafts.size() + 1) + " 题: " + String.join("; ", itemErrors));
                    }
                }

                if (problems.isEmpty()) {
                    return drafts;
                }
                errors = problems;
                LOG.warn("出题第 {} 次有 {} 题不合格", attempt, problems.size());
            } catch (Exception e) {
                errors = List.of(e.getMessage() == null ? "未知错误" : e.getMessage());
                LOG.warn("出题第 {} 次失败: {}", attempt, errors);
            }
        }
        throw new BizException(ErrorCode.SYSTEM_ERROR, "出题失败：" + String.join("; ", errors));
    }

    /**
     * 归一化模型的小毛病，而不是让前端去兼容：
     * - 选项自带「A. 」前缀，去掉，否则前端会显示成「A. A. …」；
     * - 证据里的 material_id 由服务端覆盖，防止模型编造。
     */
    private void normalize(ObjectNode node, Long materialId) {
        JsonNode options = node.path("options");
        if (options instanceof com.fasterxml.jackson.databind.node.ArrayNode array) {
            for (int i = 0; i < array.size(); i++) {
                JsonNode option = array.get(i);
                if (option.isTextual()) {
                    String text = option.asText().replaceFirst("^[A-Ha-h][.、)]\\s*", "");
                    array.set(i, objectMapper.getNodeFactory().textNode(text));
                }
            }
        }

        if (materialId != null) {
            for (JsonNode evidence : node.path("evidence")) {
                if (evidence instanceof ObjectNode ev) {
                    ev.put("material_id", materialId);
                }
            }
        }
    }

    /** 结构校验之外，再查业务规则 */
    private List<String> validateDraft(ObjectNode node, Set<Long> allowedIds, String expectedType,
                                       List<DraftGenerateRequest.EvidenceRef> provided) {
        List<String> errors = new ArrayList<>(JsonSchemaValidator.validate("question-draft", node));

        if (!expectedType.equals(node.path("type").asText())) {
            errors.add("题型应为 " + expectedType);
        }

        // 证据必须来自调用方给的原文，不允许模型自己编
        if (provided == null || provided.isEmpty()) {
            errors.add("没有提供证据来源，不能出题");
        } else {
            for (JsonNode evidence : node.path("evidence")) {
                int page = evidence.path("page").asInt();
                String quote = evidence.path("quote").asText("");
                boolean matched = provided.stream().anyMatch(p ->
                        p.getPage() != null && p.getPage() == page
                                && p.getQuote() != null && quoteMatch(p.getQuote(), quote));
                if (!matched) {
                    errors.add("证据与原文不符：第 " + page + " 页「" + quote + "」");
                }
            }
        }

        Set<Long> used = new HashSet<>();
        for (JsonNode k : node.path("knowledge")) {
            long id = k.path("knowledge_id").asLong();
            if (!allowedIds.contains(id)) {
                errors.add("引用了给定列表外的知识点 " + id);
            }
            used.add(id);
        }

        double total = node.path("total_score").asDouble();
        JsonNode subs = node.path("sub_questions");
        if (subs.isArray() && !subs.isEmpty()) {
            double sum = 0;
            for (JsonNode sub : subs) {
                sum += sub.path("score").asDouble();
                double rubricSum = 0;
                for (JsonNode point : sub.path("rubric")) {
                    rubricSum += point.path("score").asDouble();
                }
                if (Math.abs(rubricSum - sub.path("score").asDouble()) > 0.01) {
                    errors.add("小题 " + sub.path("no").asInt() + " 的评分点分值之和与小题分不符");
                }
            }
            if (Math.abs(sum - total) > 0.01) {
                errors.add("小问分值之和与总分不符");
            }
        }

        if ("COMPOSITE".equals(expectedType) && used.size() < 5) {
            errors.add("综合题至少要挂 5 个知识点，当前 " + used.size());
        }
        return errors;
    }

    /** 证据片段允许有省略和标点差异，用去空白后的包含关系判断 */
    private boolean quoteMatch(String source, String quote) {
        String a = source.replaceAll("\\s+", "");
        String b = quote.replaceAll("\\s+", "");
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        return a.contains(b) || b.contains(a);
    }

    private String buildPrompt(DraftGenerateRequest request) {
        String kps = request.getKnowledge().stream()
                .map(k -> "- id=" + k.getId() + "：" + k.getName())
                .collect(Collectors.joining("\n"));

        String evidence = "";
        if (request.getEvidence() != null && !request.getEvidence().isEmpty()) {
            evidence = request.getEvidence().stream()
                    .map(e -> "- 知识点 " + e.getKnowledgeId() + "：第 " + e.getPage() + " 页「"
                            + (e.getQuote() == null ? "" : e.getQuote()) + "」信号 " + e.getSignal())
                    .collect(Collectors.joining("\n", "原文证据：\n", "\n"));
        }

        return """
                请基于下面的知识点出 %d 道题，题型为 %s。
                每道题输出一个对象，全部放在一个 JSON 数组里，结构：
                {
                  "type": "%s",
                  "stem": "题干",
                  "options": ["选项A","选项B"],
                  "answer": ["A"],
                  "total_score": 2,
                  "difficulty": 1~5,
                  "bloom": "REMEMBER|UNDERSTAND|APPLY|ANALYZE",
                  "source": "AI",
                  "knowledge": [{"knowledge_id": 数字, "weight": 0~1, "role": "PRIMARY|RELATED|CONFUSABLE"}],
                  "sub_questions": [{"no":1,"stem":"...","score":8,"answer":"...","rubric":[{"point":"...","score":4,"knowledge_id":数字}]}],
                  "evidence": [{"material_id": 1, "page": 1, "quote": "原文片段"}],
                  "explain": "为什么出这道题（结合证据）"
                }

                规则：
                - 只能使用下面列出的 knowledge_id；
                - 单选/判断 1 个知识点，多选 2~3 个，综合题 5~8 个（1 主考点 + 若干关联 + 1 易混）；
                - 综合题必须有小问 sub_questions，小问分值之和等于 total_score，每个小题的评分点分值之和等于该小题分值；
                - evidence 至少一条，页码和原文要来自上面的证据；
                - 题干用中文，选择题提供 4 个选项。

                可用知识点：
                %s
                %s
                """.formatted(request.getCount(), request.getType(), request.getType(), kps, evidence);
    }

    private String retryHint(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "";
        }
        return "\n\n上一次输出有不合格项，请修正后重新输出完整数组：\n- " + String.join("\n- ", errors);
    }

    private String stripFence(String content) {
        String text = content == null ? "" : content.trim();
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
