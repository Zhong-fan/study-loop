package com.studyloop.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyloop.ai.client.LlmClient;
import com.studyloop.ai.config.AiProperties;
import com.studyloop.ai.dto.GenerateRequest;
import com.studyloop.ai.dto.GradeRequest;
import com.studyloop.ai.entity.AiLog;
import com.studyloop.ai.mapper.AiLogMapper;
import com.studyloop.ai.service.AiService;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger LOG = LoggerFactory.getLogger(AiServiceImpl.class);

    private static final String SYSTEM_PROMPT = """
            你是一位大学课程助教，负责根据知识点出题和判分。
            你只输出 JSON，不要输出解释、不要用 markdown 代码块包裹。
            """;

    private final LlmClient llmClient;
    private final AiProperties properties;
    private final AiLogMapper aiLogMapper;
    private final ObjectMapper objectMapper;

    public AiServiceImpl(LlmClient llmClient, AiProperties properties,
                         AiLogMapper aiLogMapper, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.properties = properties;
        this.aiLogMapper = aiLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Map<String, Object>> generateQuestions(Long userId, GenerateRequest request) {
        requireUsable();

        String userPrompt = """
                知识点：%s
                题型：%s
                数量：%d
                请生成题目，JSON 数组格式，每项包含字段：
                stem（题干）、options（选项数组，判断题和填空题为空数组）、
                answer（答案数组，选择题填选项字母）、analysis（解析）。
                """.formatted(request.getKnowledgeName(), request.getType(), request.getCount());

        long start = System.currentTimeMillis();
        try {
            String content = llmClient.chat(SYSTEM_PROMPT, userPrompt);
            JsonNode root = objectMapper.readTree(stripFence(content));

            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode node : root) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("stem", node.path("stem").asText());
                item.put("options", normalizeOptions(node.path("options")));
                item.put("answer", normalizeAnswers(node.path("answer")));
                item.put("analysis", node.path("analysis").asText());
                result.add(item);
            }

            writeLog(userId, "GENERATE_QUESTION", request.getKnowledgeId(), start, true, null);
            return result;
        } catch (Exception e) {
            writeLog(userId, "GENERATE_QUESTION", request.getKnowledgeId(), start, false, e.getMessage());
            throw new BizException(ErrorCode.SYSTEM_ERROR, "生成题目失败：" + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> gradeSubjective(Long userId, GradeRequest request) {
        requireUsable();

        String userPrompt = """
                题目：%s
                参考答案或要点：%s
                学生作答：%s
                请判分并输出 JSON：{"score": 0~1 的小数, "comment": "一句话点评"}。
                """.formatted(request.getStem(), request.getReference(), request.getAnswer());

        long start = System.currentTimeMillis();
        try {
            String content = llmClient.chat(SYSTEM_PROMPT, userPrompt);
            JsonNode root = objectMapper.readTree(stripFence(content));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("score", root.path("score").asDouble(0));
            result.put("comment", root.path("comment").asText(""));

            writeLog(userId, "GRADE_SUBJECTIVE", request.getKnowledgeId(), start, true, null);
            return result;
        } catch (Exception e) {
            writeLog(userId, "GRADE_SUBJECTIVE", request.getKnowledgeId(), start, false, e.getMessage());
            throw new BizException(ErrorCode.SYSTEM_ERROR, "判分失败：" + e.getMessage());
        }
    }

    /** 模型有时会写 "A. 选项内容"，这里去掉字母前缀，和后端存储格式一致 */
    private List<String> normalizeOptions(JsonNode options) {
        List<String> result = new ArrayList<>();
        if (options == null || !options.isArray()) {
            return result;
        }
        for (JsonNode node : options) {
            String text = node.asText("").trim().replaceFirst("^[A-Fa-f][.、)）]\\s*", "");
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        return result;
    }

    /** 答案统一成大写字母 / 文本，去掉多余符号 */
    private List<String> normalizeAnswers(JsonNode answers) {
        List<String> result = new ArrayList<>();
        if (answers == null || !answers.isArray()) {
            return result;
        }
        for (JsonNode node : answers) {
            String text = node.asText("").trim().replaceFirst("^[A-Fa-f][.、)）]\\s*", "");
            if (!text.isEmpty()) {
                result.add(text.toUpperCase());
            }
        }
        return result;
    }

    private void requireUsable() {
        if (!properties.usable()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR,
                    "AI 未配置：设置环境变量 AI_API_KEY 并把 app.ai.enabled 打开");
        }
    }

    /** 模型偶尔会套一层 ```json，这里剥掉 */
    private String stripFence(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return text;
    }

    private void writeLog(Long userId, String scene, Long knowledgeId,
                          long start, boolean success, String error) {
        try {
            AiLog log = new AiLog();
            log.setUserId(userId);
            log.setScene(scene);
            log.setKnowledgeId(knowledgeId);
            log.setModel(properties.getModel());
            log.setCostMs(System.currentTimeMillis() - start);
            log.setSuccess(success ? 1 : 0);
            log.setErrorMsg(error == null ? null : error.substring(0, Math.min(error.length(), 250)));
            log.setCreatedAt(LocalDateTime.now());
            aiLogMapper.insert(log);
        } catch (Exception e) {
            LOG.warn("写 AI 调用记录失败: {}", e.getMessage());
        }
    }
}
