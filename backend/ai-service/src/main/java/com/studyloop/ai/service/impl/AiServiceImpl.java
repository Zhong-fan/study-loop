package com.studyloop.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyloop.ai.client.LlmClient;
import com.studyloop.ai.config.AiProperties;
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
    public Map<String, Object> gradeSubjective(Long userId, GradeRequest request) {
        requireUsable();

        String userPrompt = """
                题目：%s
                参考答案或要点：%s
                学生作答：%s
                请按评分点逐条判断，输出 JSON：
                {"score": 0~1 的小数, "comment": "一句话点评", "points": [{"point": "评分点原文", "hit": true 或 false, "comment": "该点说明"}]}
                没有评分点时 points 输出空数组。
                """.formatted(request.getStem(), request.getReference(), request.getAnswer());

        long start = System.currentTimeMillis();
        try {
            String content = llmClient.chat(SYSTEM_PROMPT, userPrompt);
            JsonNode root = objectMapper.readTree(stripFence(content));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("score", root.path("score").asDouble(0));
            result.put("comment", root.path("comment").asText(""));

            List<Map<String, Object>> points = new ArrayList<>();
            for (JsonNode node : root.path("points")) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("point", node.path("point").asText(""));
                point.put("hit", node.path("hit").asBoolean(false));
                point.put("comment", node.path("comment").asText(""));
                points.add(point);
            }
            result.put("points", points);

            writeLog(userId, "GRADE_SUBJECTIVE", request.getKnowledgeId(), start, true, null);
            return result;
        } catch (Exception e) {
            writeLog(userId, "GRADE_SUBJECTIVE", request.getKnowledgeId(), start, false, e.getMessage());
            throw new BizException(ErrorCode.SYSTEM_ERROR, "判分失败：" + e.getMessage());
        }
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
