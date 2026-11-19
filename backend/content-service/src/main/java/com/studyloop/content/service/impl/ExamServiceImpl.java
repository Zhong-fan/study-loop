package com.studyloop.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.validate.JsonSchemaValidator;
import com.studyloop.content.client.AiExamClient;
import com.studyloop.content.dto.ExamCreateRequest;
import com.studyloop.content.entity.Knowledge;
import com.studyloop.content.entity.KnowledgeAlias;
import com.studyloop.content.entity.MaterialPage;
import com.studyloop.content.entity.RealExam;
import com.studyloop.content.entity.RealExamImport;
import com.studyloop.content.entity.RealExamItem;
import com.studyloop.content.mapper.KnowledgeAliasMapper;
import com.studyloop.content.mapper.KnowledgeMapper;
import com.studyloop.content.mapper.MaterialPageMapper;
import com.studyloop.content.mapper.RealExamImportMapper;
import com.studyloop.content.mapper.RealExamItemMapper;
import com.studyloop.content.mapper.RealExamMapper;
import com.studyloop.content.service.ExamService;
import com.studyloop.content.support.ExamStemNormalizer;
import com.studyloop.content.support.KnowledgeNameNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExamServiceImpl implements ExamService {

    private static final Logger LOG = LoggerFactory.getLogger(ExamServiceImpl.class);

    private final RealExamMapper examMapper;
    private final RealExamItemMapper itemMapper;
    private final RealExamImportMapper importMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeAliasMapper aliasMapper;
    private final MaterialPageMapper pageMapper;
    private final AiExamClient aiExamClient;

    public ExamServiceImpl(RealExamMapper examMapper, RealExamItemMapper itemMapper,
                           RealExamImportMapper importMapper,
                           KnowledgeMapper knowledgeMapper, KnowledgeAliasMapper aliasMapper,
                           MaterialPageMapper pageMapper, AiExamClient aiExamClient) {
        this.examMapper = examMapper;
        this.itemMapper = itemMapper;
        this.importMapper = importMapper;
        this.knowledgeMapper = knowledgeMapper;
        this.aliasMapper = aliasMapper;
        this.pageMapper = pageMapper;
        this.aiExamClient = aiExamClient;
    }

    @Override
    public Long create(Long userId, ExamCreateRequest request) {
        if (request.getCourseId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择课程");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "试卷名称不能为空");
        }
        RealExam exam = new RealExam();
        exam.setCourseId(request.getCourseId());
        exam.setTitle(request.getTitle().trim());
        exam.setExamYear(request.getExamYear());
        exam.setSchool(request.getSchool());
        exam.setMaterialId(request.getMaterialId());
        exam.setCreatedBy(userId);
        exam.setCreatedAt(LocalDateTime.now());
        exam.setDeleted(0);
        examMapper.insert(exam);
        return exam.getId();
    }

    @Override
    public List<Map<String, Object>> list(Long userId, Long courseId) {
        QueryWrapper<RealExam> wrapper = new QueryWrapper<>();
        wrapper.eq("created_by", userId);
        wrapper.eq("deleted", 0);
        if (courseId != null) {
            wrapper.eq("course_id", courseId);
        }
        wrapper.orderByDesc("exam_year").orderByDesc("id");

        List<Map<String, Object>> result = new ArrayList<>();
        for (RealExam exam : examMapper.selectList(wrapper)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", exam.getId());
            item.put("courseId", exam.getCourseId());
            item.put("title", exam.getTitle());
            item.put("examYear", exam.getExamYear());
            item.put("school", exam.getSchool());
            item.put("materialId", exam.getMaterialId());
            item.put("itemCount", itemMapper.selectCount(new QueryWrapper<RealExamItem>()
                    .eq("exam_id", exam.getId())
                    .eq("deleted", 0)));
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        RealExam exam = owned(userId, id);
        exam.setDeleted(1);
        examMapper.updateById(exam);

        // 题目跟着软删，避免"试卷能恢复、题目没了"
        itemMapper.update(null, new UpdateWrapper<RealExamItem>()
                .eq("exam_id", id)
                .set("deleted", 1));
    }

    @Override
    @Transactional
    public Map<String, Object> importItems(Long userId, Long examId, Long materialId) {
        RealExam exam = owned(userId, examId);

        // 同一份资料只导一次：AI 每次改写题干，靠题干去重挡不住重复导入
        if (importMapper.selectCount(new QueryWrapper<RealExamImport>()
                .eq("exam_id", examId)
                .eq("material_id", materialId)) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "这份资料已经导入过这张试卷，如需重导请先删除试卷");
        }

        List<MaterialPage> pages = pageMapper.selectList(new QueryWrapper<MaterialPage>()
                .eq("material_id", materialId)
                .eq("user_id", userId)
                .eq("parse_status", "SUCCESS")
                .orderByAsc("page_no"));
        List<MaterialPage> usable = pages.stream()
                .filter(p -> p.getContent() != null && !p.getContent().isBlank())
                .toList();
        if (usable.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "这份资料还没有可用的解析结果");
        }

        List<Map<String, Object>> payload = new ArrayList<>();
        for (MaterialPage page : usable) {
            Map<String, Object> item = new HashMap<>();
            item.put("page", page.getPageNo());
            item.put("text", page.getContent());
            payload.add(item);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("pages", payload);

        List<JsonNode> nodes;
        try {
            nodes = aiExamClient.extract(userId, body).getData();
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "真题抽取失败：" + e.getMessage());
        }
        if (nodes == null || nodes.isEmpty()) {
            return Map.of("imported", 0, "skipped", 0, "message", "没有识别到题目");
        }

        // 知识点索引：名称键 + 别名，和去重规则保持一致
        Map<String, Knowledge> keyIndex = buildKnowledgeIndex(exam.getCourseId());

        int seq = Math.toIntExact(itemMapper.selectCount(new QueryWrapper<RealExamItem>()
                .eq("exam_id", examId)
                .eq("deleted", 0))) + 1;
        int imported = 0;
        int skipped = 0;

        for (JsonNode node : nodes) {
            if (!(node instanceof ObjectNode objectNode)) {
                skipped++;
                continue;
            }
            List<String> errors = JsonSchemaValidator.validate("exam-item", node);
            if (!errors.isEmpty()) {
                LOG.warn("真题条目不合格，跳过：{}", errors);
                skipped++;
                continue;
            }

            String stem = node.path("stem").asText("");
            String stemKey = ExamStemNormalizer.key(stem);
            if (stemKey.isEmpty()) {
                skipped++;
                continue;
            }
            if (itemMapper.selectCount(new QueryWrapper<RealExamItem>()
                    .eq("exam_id", examId)
                    .eq("stem_key", stemKey)) > 0) {
                // 同一份卷子重复导入，直接跳过
                skipped++;
                continue;
            }

            RealExamItem item = new RealExamItem();
            item.setExamId(examId);
            item.setSeq(seq);
            item.setItemType(node.path("type").asText("SINGLE"));
            item.setStem(stem);
            item.setStemKey(stemKey);
            item.setAnswer(node.path("answer").asText(""));
            item.setScore(BigDecimal.valueOf(node.path("score").asDouble(2)));
            item.setPageNo(node.path("page").asInt(1));
            item.setDeleted(0);
            item.setCreatedAt(LocalDateTime.now());

            String knowledgeName = node.path("knowledge").asText("");
            if (!knowledgeName.isBlank()) {
                Knowledge matched = keyIndex.get(KnowledgeNameNormalizer.key(knowledgeName));
                if (matched != null) {
                    item.setKnowledgeId(matched.getId());
                    item.setChapterId(matched.getChapterId());
                }
            }
            try {
                itemMapper.insert(item);
                seq++;
                imported++;
            } catch (DuplicateKeyException e) {
                skipped++;
            }
        }

        if (imported > 0) {
            RealExamImport record = new RealExamImport();
            record.setExamId(examId);
            record.setMaterialId(materialId);
            record.setItemCount(imported);
            record.setCreatedAt(LocalDateTime.now());
            try {
                importMapper.insert(record);
            } catch (DuplicateKeyException e) {
                LOG.debug("导入记录已存在：exam={}, material={}", examId, materialId);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("materialId", materialId);
        return result;
    }

    @Override
    public Map<String, Object> distribution(Long userId, Long examId) {
        owned(userId, examId);
        List<RealExamItem> items = itemMapper.selectList(new QueryWrapper<RealExamItem>()
                .eq("exam_id", examId)
                .eq("deleted", 0));

        Map<Long, int[]> countByKnowledge = new LinkedHashMap<>();
        Map<Long, BigDecimal> scoreByKnowledge = new LinkedHashMap<>();
        Map<Long, Long> chapterOf = new HashMap<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        int linked = 0;

        for (RealExamItem item : items) {
            if (item.getScore() != null) {
                totalScore = totalScore.add(item.getScore());
            }
            Long knowledgeId = item.getKnowledgeId();
            if (knowledgeId == null) {
                continue;
            }
            linked++;
            countByKnowledge.computeIfAbsent(knowledgeId, k -> new int[1])[0]++;
            scoreByKnowledge.merge(knowledgeId,
                    item.getScore() == null ? BigDecimal.ZERO : item.getScore(), BigDecimal::add);
            if (item.getChapterId() != null) {
                chapterOf.putIfAbsent(knowledgeId, item.getChapterId());
            }
        }

        Map<Long, String> names = new HashMap<>();
        if (!countByKnowledge.isEmpty()) {
            for (Knowledge knowledge : knowledgeMapper.selectBatchIds(countByKnowledge.keySet())) {
                names.put(knowledge.getId(), knowledge.getName());
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        countByKnowledge.forEach((knowledgeId, count) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("knowledgeId", knowledgeId);
            row.put("knowledgeName", names.getOrDefault(knowledgeId, "知识点 " + knowledgeId));
            row.put("chapterId", chapterOf.get(knowledgeId));
            row.put("count", count[0]);
            row.put("score", scoreByKnowledge.getOrDefault(knowledgeId, BigDecimal.ZERO));
            rows.add(row);
        });
        rows.sort((a, b) -> Integer.compare((int) b.get("count"), (int) a.get("count")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalQuestions", items.size());
        result.put("linkedQuestions", linked);
        result.put("totalScore", totalScore);
        result.put("items", rows);
        return result;
    }

    @Override
    public List<Map<String, Object>> weighted(Long userId, Long courseId) {
        List<RealExam> exams = examMapper.selectList(new QueryWrapper<RealExam>()
                .eq("course_id", courseId)
                .eq("created_by", userId)
                .eq("deleted", 0));
        if (exams.isEmpty()) {
            return List.of();
        }
        List<Long> examIds = exams.stream().map(RealExam::getId).toList();
        List<RealExamItem> items = itemMapper.selectList(new QueryWrapper<RealExamItem>()
                .in("exam_id", examIds)
                .eq("deleted", 0)
                .isNotNull("knowledge_id"));

        Map<Long, int[]> count = new LinkedHashMap<>();
        Map<Long, BigDecimal> score = new LinkedHashMap<>();
        for (RealExamItem item : items) {
            count.computeIfAbsent(item.getKnowledgeId(), k -> new int[1])[0]++;
            score.merge(item.getKnowledgeId(),
                    item.getScore() == null ? BigDecimal.ZERO : item.getScore(), BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        count.forEach((knowledgeId, c) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("knowledgeId", knowledgeId);
            row.put("examCount", c[0]);
            row.put("examScore", score.getOrDefault(knowledgeId, BigDecimal.ZERO));
            result.add(row);
        });
        return result;
    }

    private Map<String, Knowledge> buildKnowledgeIndex(Long courseId) {
        Map<String, Knowledge> index = new HashMap<>();
        List<Knowledge> knowledges = knowledgeMapper.selectList(new QueryWrapper<Knowledge>()
                .eq("course_id", courseId)
                .eq("deleted", 0));
        Map<Long, Knowledge> byId = new HashMap<>();
        for (Knowledge knowledge : knowledges) {
            byId.put(knowledge.getId(), knowledge);
            String key = knowledge.getNameKey() == null || knowledge.getNameKey().isBlank()
                    ? KnowledgeNameNormalizer.key(knowledge.getName())
                    : knowledge.getNameKey();
            index.putIfAbsent(key, knowledge);
        }
        if (!byId.isEmpty()) {
            for (KnowledgeAlias alias : aliasMapper.selectList(
                    new QueryWrapper<KnowledgeAlias>().in("knowledge_id", byId.keySet()))) {
                Knowledge owner = byId.get(alias.getKnowledgeId());
                if (owner != null) {
                    index.putIfAbsent(alias.getAliasKey(), owner);
                }
            }
        }
        return index;
    }

    private RealExam owned(Long userId, Long id) {
        RealExam exam = examMapper.selectById(id);
        if (exam == null || Integer.valueOf(1).equals(exam.getDeleted())) {
            throw new BizException(ErrorCode.NOT_FOUND, "试卷不存在");
        }
        if (!userId.equals(exam.getCreatedBy())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权访问这份试卷");
        }
        return exam;
    }
}
