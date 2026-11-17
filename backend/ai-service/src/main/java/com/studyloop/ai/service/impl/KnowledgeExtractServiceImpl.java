package com.studyloop.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studyloop.ai.client.LlmClient;
import com.studyloop.ai.config.AiProperties;
import com.studyloop.ai.dto.KnowledgeExtractRequest;
import com.studyloop.ai.service.KnowledgeExtractService;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.validate.JsonSchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识点抽取：给几页原文，产出候选知识点。
 * <p>
 * 每条候选都必须带证据（页码 + 原文），指不回原文的直接丢弃。
 * 输出不合格时把校验错误拼回 prompt 重试一次。
 */
@Service
public class KnowledgeExtractServiceImpl implements KnowledgeExtractService {

    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeExtractServiceImpl.class);

    private static final String SYSTEM_PROMPT = """
            你是一位课程知识点抽取器。你只输出 JSON 数组，不要解释，不要 markdown 代码块。
            每个元素都必须能从原文找到依据；找不到依据的不要输出。
            """;

    private final LlmClient llmClient;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public KnowledgeExtractServiceImpl(LlmClient llmClient, AiProperties properties, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<JsonNode> extract(KnowledgeExtractRequest request) {
        if (!properties.usable()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "AI 未配置，无法抽取知识点");
        }

        List<String> errors = List.of();
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String content = llmClient.chat(SYSTEM_PROMPT, buildPrompt(request) + retryHint(errors));
                JsonNode root = objectMapper.readTree(stripFence(content));
                if (!root.isArray()) {
                    errors = List.of("根节点必须是数组");
                    continue;
                }

                List<JsonNode> valid = new ArrayList<>();
                List<String> itemErrors = new ArrayList<>();
                for (JsonNode item : root) {
                    if (!(item instanceof ObjectNode node)) {
                        itemErrors.add("元素不是对象");
                        continue;
                    }
                    fillMaterial(node, request.getMaterialId());
                    List<String> itemError = JsonSchemaValidator.validate("knowledge-candidate", node);
                    if (itemError.isEmpty()) {
                        valid.add(node);
                    } else {
                        itemErrors.add(node.path("name").asText("?") + ": " + String.join("; ", itemError));
                    }
                }

                if (itemErrors.isEmpty()) {
                    return valid;
                }
                errors = itemErrors;
                LOG.warn("知识点抽取第 {} 次有 {} 条不合格", attempt, itemErrors.size());
            } catch (Exception e) {
                errors = List.of(e.getMessage() == null ? "未知错误" : e.getMessage());
                LOG.warn("知识点抽取第 {} 次失败: {}", attempt, errors);
            }
        }

        throw new BizException(ErrorCode.SYSTEM_ERROR, "知识点抽取失败：" + String.join("; ", errors));
    }

    private void fillMaterial(ObjectNode node, Long materialId) {
        JsonNode evidence = node.path("evidence");
        if (evidence.isArray()) {
            for (JsonNode item : evidence) {
                if (item instanceof ObjectNode ev && materialId != null) {
                    ev.put("material_id", materialId);
                }
            }
        }
    }

    private String buildPrompt(KnowledgeExtractRequest request) {
        String pages = request.getPages().stream()
                .map(p -> "【第 " + p.getPage() + " 页】\n" + p.getText())
                .collect(Collectors.joining("\n\n"));

        return """
                从下面的课程资料里抽取知识点，输出 JSON 数组，每个元素结构：
                {
                  "name": "知识点名称（2~80 字）",
                  "aliases": ["别名"],
                  "chapter_title": "章节标题（能从原文看出才填）",
                  "parent_name": "上级知识点（可空）",
                  "type": "CONCEPT|METHOD|CALC|PROOF|CASE",
                  "bloom": "REMEMBER|UNDERSTAND|APPLY|ANALYZE",
                  "evidence": [{"material_id": %d, "page": 页码, "quote": "原文片段", "signal": "KEYWORD|VISUAL|REPEAT|EXAMPLE|OUTLINE"}],
                  "confidence": 0.0~1.0
                }

                规则：
                - 一个知识点至少一条 evidence，quote 必须是原文里真实出现的片段；
                - 出现「重点/必考/掌握」的知识点，confidence 给高一些，signal 用 KEYWORD；
                - 不要把整章标题当成知识点，粒度控制在「一节/一个概念/一种方法」；
                - 没有依据的不要输出，宁可少抽。

                资料内容：
                %s
                """.formatted(request.getMaterialId() == null ? 0 : request.getMaterialId(), pages);
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
