package com.studyloop.practice.service;

import com.studyloop.common.result.Result;
import com.studyloop.practice.client.ContentWeightClient;
import com.studyloop.practice.entity.Question;
import com.studyloop.practice.entity.QuestionKnowledge;
import com.studyloop.practice.mapper.QuestionKnowledgeMapper;
import com.studyloop.practice.mapper.QuestionMapper;
import com.studyloop.practice.vo.PredictItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预测卷组卷。
 * <p>
 * 思路：知识点权重来自证据（强调词、例题、出现次数、重要度），
 * 按权重从高到低挑知识点，再挂上这些知识点的题目，直到凑够题量。
 * 每道题都带 reason，说明"为什么预测这道"。
 */
@Service
public class PredictService {

    private static final Logger LOG = LoggerFactory.getLogger(PredictService.class);

    private final ContentWeightClient contentClient;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final QuestionMapper questionMapper;

    public PredictService(ContentWeightClient contentClient, QuestionKnowledgeMapper questionKnowledgeMapper,
                          QuestionMapper questionMapper) {
        this.contentClient = contentClient;
        this.questionKnowledgeMapper = questionKnowledgeMapper;
        this.questionMapper = questionMapper;
    }

    public Map<String, Object> build(Long userId, Long courseId, int size) {
        int limit = Math.max(1, Math.min(size, 30));

        List<Map<String, Object>> weights;
        try {
            Result<List<Map<String, Object>>> response = contentClient.weighted(userId, courseId);
            weights = response == null ? List.of() : response.getData();
        } catch (Exception e) {
            LOG.warn("取知识点权重失败: {}", e.getMessage());
            weights = List.of();
        }

        Map<String, Object> result = new HashMap<>();
        if (weights == null || weights.isEmpty()) {
            result.put("items", List.of());
            result.put("message", "还没有知识点权重，先解析资料并抽取知识点");
            return result;
        }

        Map<Long, Map<String, Object>> weightById = new LinkedHashMap<>();
        for (Map<String, Object> item : weights) {
            Object id = item.get("knowledgeId");
            if (id != null) {
                weightById.put(Long.valueOf(String.valueOf(id)), item);
            }
        }

        // 一次查出这些知识点关联的题目
        List<QuestionKnowledge> links = questionKnowledgeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<QuestionKnowledge>()
                        .in("knowledge_id", weightById.keySet()));
        Map<Long, List<Long>> questionToKnowledge = new HashMap<>();
        Map<Long, Double> questionWeight = new HashMap<>();
        for (QuestionKnowledge link : links) {
            questionToKnowledge.computeIfAbsent(link.getQuestionId(), k -> new ArrayList<>()).add(link.getKnowledgeId());
            Map<String, Object> weight = weightById.get(link.getKnowledgeId());
            double value = weight == null ? 0 : Double.parseDouble(String.valueOf(weight.getOrDefault("weight", 0)));
            questionWeight.merge(link.getQuestionId(), value, Math::max);
        }

        // 按权重从高到低挑题，同一知识点最多贡献 3 道，避免一张卷全是同一个点
        List<PredictItemVO> items = new ArrayList<>();
        Map<Long, Integer> perKnowledge = new HashMap<>();
        questionWeight.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .forEach(entry -> {
                    if (items.size() >= limit) {
                        return;
                    }
                    List<Long> knowledgeIds = questionToKnowledge.getOrDefault(entry.getKey(), List.of());
                    boolean skippable = knowledgeIds.stream()
                            .anyMatch(id -> perKnowledge.getOrDefault(id, 0) >= 3);
                    if (skippable) {
                        return;
                    }

                    Question question = questionMapper.selectById(entry.getKey());
                    if (question == null || Integer.valueOf(1).equals(question.getDeleted())) {
                        return;
                    }
                    for (Long id : knowledgeIds) {
                        perKnowledge.merge(id, 1, Integer::sum);
                    }

                    PredictItemVO item = new PredictItemVO();
                    item.setQuestionId(question.getId());
                    item.setType(question.getType());
                    item.setStem(question.getStem());
                    item.setKnowledgeIds(knowledgeIds);
                    item.setWeight(entry.getValue());
                    Map<String, Object> best = bestOf(knowledgeIds, weightById);
                    item.setReason(reasonOf(best));
                    item.setEvidenceQuote(String.valueOf(best.getOrDefault("topQuote", "")));
                    Object topPage = best.get("topPage");
                    item.setEvidencePage(topPage == null ? null : Integer.valueOf(String.valueOf(topPage)));
                    items.add(item);
                });

        result.put("items", items);
        result.put("total", items.size());
        return result;
    }

    /** 权重最高的那个知识点，用来解释"为什么预测这道题" */
    private Map<String, Object> bestOf(List<Long> knowledgeIds, Map<Long, Map<String, Object>> weightById) {
        if (knowledgeIds.isEmpty()) {
            return Map.of();
        }
        Long best = knowledgeIds.stream()
                .max((a, b) -> Double.compare(weight(weightById, a), weight(weightById, b)))
                .orElse(knowledgeIds.get(0));
        return weightById.getOrDefault(best, Map.of());
    }

    private String reasonOf(Map<String, Object> weight) {
        if (weight.isEmpty()) {
            return "";
        }
        String name = String.valueOf(weight.getOrDefault("name", ""));
        String pages = String.valueOf(weight.getOrDefault("pages", ""));
        Object keywords = weight.getOrDefault("keywordCount", 0);
        return "「" + name + "」权重 " + weight.getOrDefault("weight", 0)
                + (pages.isBlank() || "null".equals(pages) ? "" : "，出自第 " + pages + " 页")
                + (Integer.parseInt(String.valueOf(keywords)) > 0 ? "，含强调词" : "");
    }

    private double weight(Map<Long, Map<String, Object>> weightById, Long id) {
        Map<String, Object> item = weightById.get(id);
        return item == null ? 0 : Double.parseDouble(String.valueOf(item.getOrDefault("weight", 0)));
    }
}
