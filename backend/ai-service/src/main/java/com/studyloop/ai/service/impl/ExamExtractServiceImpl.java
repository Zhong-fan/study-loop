package com.studyloop.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studyloop.ai.client.LlmClient;
import com.studyloop.ai.config.AiProperties;
import com.studyloop.ai.service.ExamExtractService;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.validate.JsonSchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 真题抽取：和知识点抽取不同，这里要的是"题目"本身，而不是概念。
 * 仍然走 schema 校验 + 失败重试，避免模型输出半结构化文本。
 */
@Service
public class ExamExtractServiceImpl implements ExamExtractService {

    private static final Logger LOG = LoggerFactory.getLogger(ExamExtractServiceImpl.class);

    private static final String SYSTEM_PROMPT = """
            你是一位试卷结构化助手。你只输出 JSON 数组，不要解释，不要 markdown 代码块。
            只抽取页面上真实存在的题目；题干必须来自原文，不要自己编题。
            每题都要给出所在页码；找不到页码的不要输出。
            """;

    private final LlmClient llmClient;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public ExamExtractServiceImpl(LlmClient llmClient, AiProperties properties, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<JsonNode> extract(Map<String, Object> body) {
        if (!properties.usable()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "AI 未配置，无法抽取真题");
        }

        List<String> errors = List.of();
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String content = llmClient.chat(SYSTEM_PROMPT, buildPrompt(body) + retryHint(errors));
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
                    List<String> itemError = JsonSchemaValidator.validate("exam-item", node);
                    if (itemError.isEmpty()) {
                        valid.add(node);
                    } else {
                        itemErrors.add(node.path("stem").asText("?") + ": " + String.join("; ", itemError));
                    }
                }
                if (itemErrors.isEmpty()) {
                    return valid;
                }
                errors = itemErrors;
                LOG.warn("真题抽取第 {} 次有 {} 条不合格", attempt, itemErrors.size());
            } catch (Exception e) {
                errors = List.of(e.getMessage() == null ? "未知错误" : e.getMessage());
                LOG.warn("真题抽取第 {} 次失败: {}", attempt, errors);
            }
        }
        throw new BizException(ErrorCode.SYSTEM_ERROR, "真题抽取失败：" + String.join("; ", errors));
    }

    private String buildPrompt(Map<String, Object> body) {
        StringBuilder pages = new StringBuilder();
        Object raw = body.get("pages");
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> page) {
                    pages.append("【第 ").append(page.get("page")).append(" 页】\n")
                            .append(page.get("text")).append("\n\n");
                }
            }
        }
        return """
                从下面的试卷页面中抽取题目，输出 JSON 数组，每个元素字段：
                - stem: 题干原文（去掉题号）
                - type: SINGLE / MULTIPLE / JUDGE / SHORT / COMPOSITE
                - answer: 参考答案，没有就空字符串
                - score: 分值数字，看不到就按题型给默认值（单选 2、多选 3、判断 2、简答 5、综合 10）
                - page: 该题所在页码
                - knowledge: 这道题主要考的知识点名称（用原文里的说法，没有就空字符串）

                页面内容：
                %s
                """.formatted(pages);
    }

    private String retryHint(List<String> errors) {
        if (errors.isEmpty()) {
            return "";
        }
        return "\n\n上一次输出有这些问题，请修正后重新输出：\n- " + String.join("\n- ", errors);
    }

    private String stripFence(String content) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```")) {
            int first = text.indexOf('\n');
            int last = text.lastIndexOf("```");
            if (first > 0 && last > first) {
                text = text.substring(first + 1, last).trim();
            }
        }
        return text;
    }
}
