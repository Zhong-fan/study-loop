package com.studyloop.practice.service;

import com.studyloop.common.result.Result;
import com.studyloop.practice.client.ContentExamClient;
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
    private final ContentExamClient examClient;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final QuestionMapper questionMapper;

    public PredictService(ContentWeightClient contentClient, ContentExamClient examClient,
                          QuestionKnowledgeMapper questionKnowledgeMapper, QuestionMapper questionMapper) {
        this.contentClient = contentClient;
        this.examClient = examClient;
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

        // 真题里出现过的知识点，优先级再抬一点：最多 +30%
        Map<Long, Map<String, Object>> examById = new HashMap<>();
        try {
            Result<List<Map<String, Object>>> examResponse = examClient.weighted(userId, courseId);
            if (examResponse != null && examResponse.getData() != null) {
                for (Map<String, Object> row : examResponse.getData()) {
                    Object id = row.get("knowledgeId");
                    if (id != null) {
                        examById.put(Long.valueOf(String.valueOf(id)), row);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("取真题频次失败: {}", e.getMessage());
        }

        for (Map<String, Object> item : weights) {
            Object id = item.get("knowledgeId");
            if (id == null) {
                continue;
            }
            Map<String, Object> exam = examById.get(Long.valueOf(String.valueOf(id)));
            int examCount = exam == null ? 0 : Integer.parseInt(String.valueOf(exam.getOrDefault("examCount", 0)));
            item.put("examCount", examCount);
            if (examCount > 0) {
                double base = Double.parseDouble(String.valueOf(item.getOrDefault("weight", 0)));
                double boosted = Math.min(1.0, base * (1 + Math.min(0.3, examCount * 0.1)));
                item.put("weight", Math.round(boosted * 100) / 100.0);
            }
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
                    String reason = reasonOf(best);
                    int maxExam = knowledgeIds.stream().mapToInt(id -> {
                        Map<String, Object> w = weightById.get(id);
                        return w == null ? 0 : Integer.parseInt(String.valueOf(w.getOrDefault("examCount", 0)));
                    }).max().orElse(0);
                    if (maxExam > 0 && !reason.contains("真题")) {
                        reason = reason + "，关联知识点里真题出现过 " + maxExam + " 次";
                    }
                    item.setReason(reason);
                    Object quote = best.get("topQuote");
                    item.setEvidenceQuote(quote == null ? null : String.valueOf(quote));
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
        String name = String.valueOf(weight.getOrDefault("name", "这个知识点"));
        Object page = weight.get("topPage");
        String where = page == null ? "" : "，出自第 " + page + " 页";

        int keywords = Integer.parseInt(String.valueOf(weight.getOrDefault("keywordCount", 0)));
        int examples = Integer.parseInt(String.valueOf(weight.getOrDefault("exampleCount", 0)));
        String why;
        if (keywords > 0 && examples > 0) {
            why = "老师标了重点，课件里也有例题";
        } else if (keywords > 0) {
            why = "老师标了重点";
        } else if (examples > 0) {
            why = "课件里有例题";
        } else {
            why = "课件里讲到过";
        }
        int examCount = Integer.parseInt(String.valueOf(weight.getOrDefault("examCount", 0)));
        String examHint = examCount > 0 ? "，真题里出现过 " + examCount + " 次" : "";
        return "「" + name + "」" + why + where + examHint;
    }

    private double weight(Map<Long, Map<String, Object>> weightById, Long id) {
        Map<String, Object> item = weightById.get(id);
        return item == null ? 0 : Double.parseDouble(String.valueOf(item.getOrDefault("weight", 0)));
    }
}
