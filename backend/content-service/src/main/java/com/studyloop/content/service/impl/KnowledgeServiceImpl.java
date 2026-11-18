package com.studyloop.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.studyloop.common.context.UserContext;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.content.client.InsightCleanupClient;
import com.studyloop.content.dto.KnowledgeRequest;
import com.studyloop.content.entity.Chapter;
import com.studyloop.content.entity.Knowledge;
import com.studyloop.content.entity.KnowledgeEvidence;
import com.studyloop.content.mapper.ChapterMapper;
import com.studyloop.content.mapper.KnowledgeEvidenceMapper;
import com.studyloop.content.mapper.KnowledgeMapper;
import com.studyloop.content.service.KnowledgeService;
import com.studyloop.content.vo.KnowledgeEvidenceVO;
import com.studyloop.content.vo.KnowledgeVO;
import com.studyloop.content.vo.WeightedKnowledgeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识点业务。
 * <p>
 * 知识点挂在章下面，归属跟着课程走，所以每次操作都先确认章属于当前用户。
 */
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeServiceImpl.class);

    private final KnowledgeMapper knowledgeMapper;
    private final ChapterMapper chapterMapper;
    private final InsightCleanupClient insightCleanupClient;
    private final KnowledgeEvidenceMapper evidenceMapper;

    public KnowledgeServiceImpl(KnowledgeMapper knowledgeMapper, ChapterMapper chapterMapper,
                                InsightCleanupClient insightCleanupClient,
                                KnowledgeEvidenceMapper evidenceMapper) {
        this.knowledgeMapper = knowledgeMapper;
        this.chapterMapper = chapterMapper;
        this.insightCleanupClient = insightCleanupClient;
        this.evidenceMapper = evidenceMapper;
    }

    @Override
    public List<KnowledgeVO> listByChapter(Long chapterId) {
        checkChapter(chapterId);

        QueryWrapper<Knowledge> wrapper = new QueryWrapper<>();
        wrapper.eq("chapter_id", chapterId);
        wrapper.eq("deleted", 0);
        wrapper.orderByAsc("id");

        List<KnowledgeVO> result = new ArrayList<>();
        for (Knowledge knowledge : knowledgeMapper.selectList(wrapper)) {
            result.add(toVO(knowledge));
        }
        return result;
    }

    @Override
    public List<KnowledgeVO> listByCourse(Long courseId) {
        Long userId = UserContext.requireUserId();

        QueryWrapper<Knowledge> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("course_id", courseId);
        wrapper.eq("deleted", 0);
        wrapper.orderByAsc("chapter_id").orderByAsc("id");

        List<KnowledgeVO> result = new ArrayList<>();
        for (Knowledge knowledge : knowledgeMapper.selectList(wrapper)) {
            result.add(toVO(knowledge));
        }
        return result;
    }

    @Override
    public Long create(Long courseId, KnowledgeRequest request) {
        Chapter chapter = checkChapter(request.getChapterId());

        Knowledge knowledge = new Knowledge();
        knowledge.setUserId(chapter.getUserId());
        knowledge.setCourseId(courseId);
        knowledge.setChapterId(request.getChapterId());
        knowledge.setName(request.getName().trim());
        knowledge.setType(request.getType() == null ? "CONCEPT" : request.getType());
        knowledge.setImportance(request.getImportance() == null ? 3 : request.getImportance());
        knowledge.setPageNo(request.getPageNo());
        knowledge.setMaterialId(request.getMaterialId());
        knowledge.setCreatedAt(LocalDateTime.now());
        knowledge.setDeleted(0);

        knowledgeMapper.insert(knowledge);
        return knowledge.getId();
    }

    @Override
    public void update(Long id, KnowledgeRequest request) {
        Knowledge knowledge = findOwned(id);
        knowledge.setName(request.getName().trim());
        if (request.getType() != null) {
            knowledge.setType(request.getType());
        }
        if (request.getImportance() != null) {
            knowledge.setImportance(request.getImportance());
        }
        knowledge.setPageNo(request.getPageNo());
        knowledgeMapper.updateById(knowledge);
    }

    @Override
    public void delete(Long id) {
        Knowledge knowledge = findOwned(id);
        knowledge.setDeleted(1);
        knowledgeMapper.updateById(knowledge);

        // 顺手清掉掌握记录；失败只记日志，不影响删除结果
        try {
            insightCleanupClient.deleteMastery(id, knowledge.getUserId());
        } catch (Exception e) {
            LOG.warn("清理知识点掌握记录失败，knowledgeId={}: {}", id, e.getMessage());
        }
    }

    @Override
    public long countByCourse(Long courseId) {
        Long userId = UserContext.requireUserId();

        QueryWrapper<Knowledge> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("course_id", courseId);
        wrapper.eq("deleted", 0);
        return knowledgeMapper.selectCount(wrapper);
    }

    private Chapter checkChapter(Long chapterId) {
        Long userId = UserContext.requireUserId();

        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null || chapter.getDeleted() == 1 || !userId.equals(chapter.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "章节不存在");
        }
        return chapter;
    }

    private Knowledge findOwned(Long id) {
        Long userId = UserContext.requireUserId();

        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null || knowledge.getDeleted() == 1 || !userId.equals(knowledge.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "知识点不存在");
        }
        return knowledge;
    }

    private KnowledgeVO toVO(Knowledge knowledge) {
        KnowledgeVO vo = new KnowledgeVO();
        vo.setId(knowledge.getId());
        vo.setCourseId(knowledge.getCourseId());
        vo.setChapterId(knowledge.getChapterId());
        vo.setName(knowledge.getName());
        vo.setType(knowledge.getType());
        vo.setImportance(knowledge.getImportance());
        vo.setPageNo(knowledge.getPageNo());
        vo.setMaterialId(knowledge.getMaterialId());
        return vo;
    }
    @Override
    public List<KnowledgeEvidenceVO> evidenceByCourse(Long courseId) {
        List<Knowledge> list = knowledgeMapper.selectList(new QueryWrapper<Knowledge>()
                .eq("course_id", courseId)
                .eq("user_id", UserContext.requireUserId())
                .eq("deleted", 0));
        List<Long> ids = list.stream().map(Knowledge::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        List<KnowledgeEvidenceVO> result = new ArrayList<>();
        for (KnowledgeEvidence evidence : evidenceMapper.selectList(
                new QueryWrapper<KnowledgeEvidence>().in("knowledge_id", ids))) {
            KnowledgeEvidenceVO vo = new KnowledgeEvidenceVO();
            vo.setKnowledgeId(evidence.getKnowledgeId());
            vo.setMaterialId(evidence.getMaterialId());
            vo.setPageNo(evidence.getPageNo());
            vo.setQuote(evidence.getQuote());
            vo.setSignalType(evidence.getSignalType());
            vo.setWeight(evidence.getWeight());
            result.add(vo);
        }
        return result;
    }
    @Override
    public List<WeightedKnowledgeVO> weightedByCourse(Long courseId) {
        List<Knowledge> list = knowledgeMapper.selectList(new QueryWrapper<Knowledge>()
                .eq("course_id", courseId)
                .eq("user_id", UserContext.requireUserId())
                .eq("deleted", 0));

        // 一次查出所有知识点的证据，避免按知识点逐条查（N+1）
        List<Long> ids = list.stream().map(Knowledge::getId).toList();
        java.util.Map<Long, List<KnowledgeEvidence>> evidenceMap = new java.util.HashMap<>();
        if (!ids.isEmpty()) {
            for (KnowledgeEvidence evidence : evidenceMapper.selectList(
                    new QueryWrapper<KnowledgeEvidence>().in("knowledge_id", ids))) {
                evidenceMap.computeIfAbsent(evidence.getKnowledgeId(), k -> new ArrayList<>()).add(evidence);
            }
        }

        List<WeightedKnowledgeVO> result = new ArrayList<>();
        for (Knowledge knowledge : list) {
            List<KnowledgeEvidence> evidences = evidenceMap.getOrDefault(knowledge.getId(), List.of());

            int keyword = 0;
            int example = 0;
            int outline = 0;
            int repeat = 0;
            java.util.LinkedHashSet<Integer> pages = new java.util.LinkedHashSet<>();
            for (KnowledgeEvidence evidence : evidences) {
                String signal = evidence.getSignalType() == null ? "" : evidence.getSignalType();
                switch (signal) {
                    case "KEYWORD" -> keyword++;
                    case "EXAMPLE" -> example++;
                    case "OUTLINE" -> outline++;
                    case "REPEAT" -> repeat++;
                    default -> { }
                }
                if (evidence.getPageNo() != null) {
                    pages.add(evidence.getPageNo());
                }
            }

            int importance = knowledge.getImportance() == null ? 3 : knowledge.getImportance();
            double weight = com.studyloop.content.support.KnowledgeWeightCalculator.compute(
                    importance, keyword, example, repeat, evidences.size());

            WeightedKnowledgeVO vo = new WeightedKnowledgeVO();
            vo.setKnowledgeId(knowledge.getId());
            vo.setName(knowledge.getName());
            vo.setImportance(importance);
            vo.setWeight(weight);
            vo.setEvidenceCount(evidences.size());
            vo.setKeywordCount(keyword);
            vo.setExampleCount(example);
            vo.setPages(pages.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));

            // 优先挑强调词证据，其次第一条，给前端"展开原文"用
            KnowledgeEvidence top = evidences.stream()
                    .filter(e -> "KEYWORD".equals(e.getSignalType()))
                    .findFirst()
                    .orElse(evidences.isEmpty() ? null : evidences.get(0));
            if (top != null) {
                vo.setTopQuote(top.getQuote());
                vo.setTopPage(top.getPageNo());
            }
            result.add(vo);
        }

        result.sort((a, b) -> Double.compare(b.getWeight(), a.getWeight()));
        return result;
    }
}