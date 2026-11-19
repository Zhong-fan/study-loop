package com.studyloop.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.validate.JsonSchemaValidator;
import com.studyloop.content.client.AiKnowledgeClient;
import com.studyloop.content.entity.Knowledge;
import com.studyloop.content.entity.KnowledgeEvidence;
import com.studyloop.content.entity.KnowledgeAlias;
import com.studyloop.content.entity.MaterialPage;
import com.studyloop.content.mapper.KnowledgeEvidenceMapper;
import com.studyloop.content.mapper.KnowledgeAliasMapper;
import com.studyloop.content.mapper.KnowledgeMapper;
import com.studyloop.content.mapper.MaterialPageMapper;
import com.studyloop.content.service.KnowledgeExtractService;
import com.studyloop.content.support.KnowledgeNameNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 知识点自动抽取。
 * <p>
 * 流程：取已解析页 → 分批调用 ai-service → 每条候选再过一次 Schema →
 * 按课程内名称去重 → 入库并写证据。
 * 全自动，不设人工确认；重复运行不会产生重复知识点。
 */
@Service
public class KnowledgeExtractServiceImpl implements KnowledgeExtractService {

    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeExtractServiceImpl.class);

    /** 每批最多几页，控制单次 prompt 长度 */
    private static final int BATCH_SIZE = 4;

    private final MaterialPageMapper pageMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeEvidenceMapper evidenceMapper;
    private final KnowledgeAliasMapper aliasMapper;
    private final AiKnowledgeClient aiClient;
    private final ObjectMapper objectMapper;

    public KnowledgeExtractServiceImpl(MaterialPageMapper pageMapper, KnowledgeMapper knowledgeMapper,
                                       KnowledgeEvidenceMapper evidenceMapper, KnowledgeAliasMapper aliasMapper, AiKnowledgeClient aiClient,
                                       ObjectMapper objectMapper) {
        this.pageMapper = pageMapper;
        this.knowledgeMapper = knowledgeMapper;
        this.evidenceMapper = evidenceMapper;
        this.aliasMapper = aliasMapper;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> extract(Long userId, Long courseId, Long chapterId, Long materialId) {
        List<MaterialPage> pages = pageMapper.selectList(new LambdaQueryWrapper<MaterialPage>()
                .eq(MaterialPage::getMaterialId, materialId)
                .eq(MaterialPage::getUserId, userId)
                .eq(MaterialPage::getParseStatus, "SUCCESS")
                .orderByAsc(MaterialPage::getPageNo));

        List<MaterialPage> usable = pages.stream()
                .filter(p -> p.getContent() != null && !p.getContent().isBlank())
                .toList();
        if (usable.isEmpty()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "没有已解析的页面，请先执行页面解析");
        }

        // 归一化键 → 已有知识点；别名也进同一张索引
        Map<String, Knowledge> keyIndex = new HashMap<>();
        List<Knowledge> existingList = knowledgeMapper.selectList(new LambdaQueryWrapper<Knowledge>()
                .eq(Knowledge::getCourseId, courseId)
                .eq(Knowledge::getUserId, userId));
        Map<Long, Knowledge> byId = new HashMap<>();
        for (Knowledge k : existingList) {
            byId.put(k.getId(), k);
            String key = k.getNameKey() == null || k.getNameKey().isBlank()
                    ? KnowledgeNameNormalizer.key(k.getName())
                    : k.getNameKey();
            keyIndex.putIfAbsent(key, k);
        }
        if (!existingList.isEmpty()) {
            for (KnowledgeAlias alias : aliasMapper.selectList(
                    new QueryWrapper<KnowledgeAlias>().in("knowledge_id", byId.keySet()))) {
                Knowledge owner = byId.get(alias.getKnowledgeId());
                if (owner != null) {
                    keyIndex.putIfAbsent(alias.getAliasKey(), owner);
                }
            }
        }

        int created = 0;
        int duplicates = 0;
        int evidenceCount = 0;

        for (int i = 0; i < usable.size(); i += BATCH_SIZE) {
            List<MaterialPage> batch = usable.subList(i, Math.min(i + BATCH_SIZE, usable.size()));
            List<Map<String, Object>> payloadPages = new ArrayList<>();
            for (MaterialPage page : batch) {
                Map<String, Object> item = new HashMap<>();
                item.put("page", page.getPageNo());
                item.put("text", page.getContent());
                payloadPages.add(item);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("materialId", materialId);
            body.put("pages", payloadPages);

            List<JsonNode> candidates;
            try {
                candidates = aiClient.extract(userId, body).getData();
            } catch (Exception e) {
                throw new BizException(ErrorCode.SYSTEM_ERROR, "知识点抽取失败：" + e.getMessage());
            }
            if (candidates == null || candidates.isEmpty()) {
                continue;
            }

            for (JsonNode node : candidates) {
                List<String> errors = JsonSchemaValidator.validate("knowledge-candidate", node);
                if (!errors.isEmpty()) {
                    LOG.warn("知识点候选不合格，跳过：{}", errors);
                    continue;
                }
                String name = node.path("name").asText("").trim();
                if (name.isEmpty()) {
                    continue;
                }
                String key = KnowledgeNameNormalizer.key(name);
                Knowledge matched = keyIndex.get(key);
                if (matched == null) {
                    for (JsonNode alias : node.path("aliases")) {
                        if (alias.isTextual()) {
                            matched = keyIndex.get(KnowledgeNameNormalizer.key(alias.asText()));
                            if (matched != null) {
                                break;
                            }
                        }
                    }
                }
                if (matched != null) {
                    // 同一个知识点再次出现：不建新记录，把证据累加上去
                    duplicates++;
                    evidenceCount += saveEvidence(matched.getId(), materialId, node);
                    saveAliases(matched.getId(), node, keyIndex, matched);
                    continue;
                }

                Knowledge knowledge = buildKnowledge(userId, courseId, chapterId, materialId, node, name);
                knowledge.setNameKey(key);
                knowledgeMapper.insert(knowledge);
                keyIndex.put(key, knowledge);
                saveAliases(knowledge.getId(), node, keyIndex, knowledge);
                created++;
                evidenceCount += saveEvidence(knowledge.getId(), materialId, node);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("pages", usable.size());
        result.put("created", created);
        result.put("duplicates", duplicates);
        result.put("evidence", evidenceCount);
        return result;
    }

    private Knowledge buildKnowledge(Long userId, Long courseId, Long chapterId, Long materialId,
                                     JsonNode node, String name) {
        Knowledge knowledge = new Knowledge();
        knowledge.setUserId(userId);
        knowledge.setCourseId(courseId);
        knowledge.setChapterId(chapterId);
        knowledge.setName(name.length() > 80 ? name.substring(0, 80) : name);
        knowledge.setType(mapType(node.path("type").asText("CONCEPT")));
        knowledge.setImportance(toImportance(node.path("confidence").asDouble(0.6)));
        knowledge.setMaterialId(materialId);
        knowledge.setCreatedAt(LocalDateTime.now());
        knowledge.setDeleted(0);

        JsonNode evidence = node.path("evidence");
        if (evidence.isArray() && !evidence.isEmpty()) {
            knowledge.setPageNo(evidence.get(0).path("page").asInt(1));
        }
        return knowledge;
    }

    /** 把候选里的别名挂到知识点上，后续同义说法就能命中 */
    private void saveAliases(Long knowledgeId, JsonNode node, Map<String, Knowledge> keyIndex, Knowledge owner) {
        for (JsonNode alias : node.path("aliases")) {
            if (!alias.isTextual()) {
                continue;
            }
            String text = alias.asText().trim();
            if (text.isEmpty()) {
                continue;
            }
            String key = KnowledgeNameNormalizer.key(text);
            if (key.isEmpty() || keyIndex.containsKey(key)) {
                continue;
            }
            KnowledgeAlias entity = new KnowledgeAlias();
            entity.setKnowledgeId(knowledgeId);
            entity.setAlias(text);
            entity.setAliasKey(key);
            entity.setCreatedAt(LocalDateTime.now());
            try {
                aliasMapper.insert(entity);
                keyIndex.put(key, owner);
            } catch (Exception e) {
                // 并发下唯一键冲突，忽略即可
                LOG.debug("别名写入失败：{}", text);
            }
        }
    }

    private int saveEvidence(Long knowledgeId, Long materialId, JsonNode node) {
        JsonNode items = node.path("evidence");
        if (!items.isArray()) {
            return 0;
        }
        int count = 0;
        for (JsonNode item : items) {
            KnowledgeEvidence evidence = new KnowledgeEvidence();
            evidence.setKnowledgeId(knowledgeId);
            evidence.setMaterialId(materialId);
            evidence.setPageNo(item.path("page").asInt(1));
            evidence.setQuote(truncate(item.path("quote").asText(""), 500));
            evidence.setSignalType(item.path("signal").asText("KEYWORD"));
            evidence.setWeight(1.0);
            evidence.setVerified(1);
            evidence.setCreatedAt(LocalDateTime.now());
            evidenceMapper.insert(evidence);
            count++;
        }
        return count;
    }

    /** CONCEPT 保留；方法/计算/证明按 SKILL 处理，案例单独 */
    private String mapType(String type) {
        return switch (type) {
            case "CASE" -> "CASE";
            case "CONCEPT" -> "CONCEPT";
            default -> "SKILL";
        };
    }

    private int toImportance(double confidence) {
        int value = (int) Math.round(confidence * 5);
        return Math.min(5, Math.max(1, value));
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

}
