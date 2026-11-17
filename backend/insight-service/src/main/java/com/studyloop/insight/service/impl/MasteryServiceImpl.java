package com.studyloop.insight.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.result.Result;
import com.studyloop.insight.client.ContentClient;
import com.studyloop.insight.dto.AttemptRequest;
import com.studyloop.insight.entity.Mastery;
import com.studyloop.insight.mapper.MasteryMapper;
import com.studyloop.insight.schedule.ReviewScheduler;
import com.studyloop.insight.service.MasteryService;
import com.studyloop.insight.vo.CoverageVO;
import com.studyloop.insight.vo.MasteryVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MasteryServiceImpl implements MasteryService {

    private static final Logger log = LoggerFactory.getLogger(MasteryServiceImpl.class);

    /** 正确率到多少算掌握 */
    private static final double MASTERED_ACCURACY = 0.8;

    /** 至少练两次才算掌握，避免蒙对一次就绿了 */
    private static final int MASTERED_MIN_COUNT = 2;

    private final MasteryMapper masteryMapper;
    private final ContentClient contentClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.cache.due-ttl-seconds:60}")
    private long dueTtlSeconds;

    public MasteryServiceImpl(MasteryMapper masteryMapper, ContentClient contentClient,
                              StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.masteryMapper = masteryMapper;
        this.contentClient = contentClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public MasteryVO record(Long userId, Long knowledgeId, Long courseId, boolean correct) {
        Mastery mastery = findByUserAndKnowledge(userId, knowledgeId);

        if (mastery == null) {
            mastery = new Mastery();
            mastery.setUserId(userId);
            mastery.setKnowledgeId(knowledgeId);
            mastery.setCourseId(courseId);
            mastery.setTotalCount(0);
            mastery.setCorrectCount(0);
            mastery.setAccuracy(0.0);
            mastery.setStage(0);
        } else if (courseId != null) {
            // 兼容早期没有 course_id 的记录
            mastery.setCourseId(courseId);
        }

        int total = mastery.getTotalCount() + 1;
        int correctCount = mastery.getCorrectCount() + (correct ? 1 : 0);

        mastery.setTotalCount(total);
        mastery.setCorrectCount(correctCount);
        mastery.setAccuracy((double) correctCount / total);
        mastery.setStage(ReviewScheduler.nextStage(mastery.getStage(), correct));
        mastery.setNextReview(ReviewScheduler.nextDate(mastery.getStage(), LocalDate.now()));
        mastery.setUpdatedAt(LocalDateTime.now());

        if (mastery.getId() == null) {
            masteryMapper.insert(mastery);
        } else {
            masteryMapper.updateById(mastery);
        }
        evictDueCache(userId);
        return toVO(mastery);
    }

    @Override
    @Transactional
    public MasteryVO recordDetails(Long userId, Long courseId, List<AttemptRequest.Detail> details) {
        if (details == null || details.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "没有可记录的作答明细");
        }
        MasteryVO first = null;
        for (AttemptRequest.Detail detail : details) {
            MasteryVO vo = applyDetail(userId, courseId, detail);
            if (first == null) {
                first = vo;
            }
        }
        evictDueCache(userId);
        return first;
    }

    /**
     * 单点题只动 surface，综合题只动 deep。
     * 两轨都要有数据才判"会背不会用"：单点正确率高、综合得分明显低。
     */
    private MasteryVO applyDetail(Long userId, Long courseId, AttemptRequest.Detail detail) {
        Mastery mastery = findByUserAndKnowledge(userId, detail.getKnowledgeId());
        if (mastery == null) {
            mastery = new Mastery();
            mastery.setUserId(userId);
            mastery.setKnowledgeId(detail.getKnowledgeId());
            mastery.setCourseId(courseId);
            mastery.setTotalCount(0);
            mastery.setCorrectCount(0);
            mastery.setAccuracy(0.0);
            mastery.setStage(0);
            mastery.setSurfaceAccuracy(0.0);
            mastery.setDeepAccuracy(0.0);
            mastery.setSurfaceCount(0);
            mastery.setDeepCount(0);
            mastery.setTransferFlag(0);
        } else if (courseId != null) {
            mastery.setCourseId(courseId);
        }

        double score = detail.getScore() == null ? 0 : Math.max(0, Math.min(1, detail.getScore()));
        boolean single = !"COMPOSITE".equalsIgnoreCase(detail.getContext());

        if (single) {
            int count = (mastery.getSurfaceCount() == null ? 0 : mastery.getSurfaceCount()) + 1;
            double acc = (value(mastery.getSurfaceAccuracy()) * (count - 1) + score) / count;
            mastery.setSurfaceCount(count);
            mastery.setSurfaceAccuracy(acc);
        } else {
            int count = (mastery.getDeepCount() == null ? 0 : mastery.getDeepCount()) + 1;
            double acc = (value(mastery.getDeepAccuracy()) * (count - 1) + score) / count;
            mastery.setDeepCount(count);
            mastery.setDeepAccuracy(acc);
        }

        int surfaceCount = mastery.getSurfaceCount() == null ? 0 : mastery.getSurfaceCount();
        int deepCount = mastery.getDeepCount() == null ? 0 : mastery.getDeepCount();
        double overall = (value(mastery.getSurfaceAccuracy()) * surfaceCount
                + value(mastery.getDeepAccuracy()) * deepCount) / Math.max(1, surfaceCount + deepCount);

        boolean transfer = surfaceCount >= 2 && deepCount >= 1
                && value(mastery.getSurfaceAccuracy()) - value(mastery.getDeepAccuracy()) >= 0.3;

        mastery.setAccuracy(overall);
        mastery.setTotalCount(mastery.getTotalCount() + 1);
        mastery.setCorrectCount(mastery.getCorrectCount() + (score >= 1 ? 1 : 0));
        mastery.setTransferFlag(transfer ? 1 : 0);

        int stage = mastery.getStage() == null ? 0 : mastery.getStage();
        if (score >= 1 && !transfer) {
            stage = ReviewScheduler.nextStage(stage, true);
        } else if (score < 0.5) {
            stage = 0;
        }
        mastery.setStage(stage);
        mastery.setNextReview(ReviewScheduler.nextDate(stage, LocalDate.now()));
        mastery.setUpdatedAt(LocalDateTime.now());

        if (mastery.getId() == null) {
            masteryMapper.insert(mastery);
        } else {
            masteryMapper.updateById(mastery);
        }
        return toVO(mastery);
    }

    private double value(Double number) {
        return number == null ? 0.0 : number;
    }

    @Override
    public List<MasteryVO> due(Long userId) {
        String key = dueKey(userId);

        // 先读缓存；Redis 出任何问题都退回数据库，不把缓存当唯一数据源
        if (cacheEnabled) {
            try {
                String cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    return objectMapper.readValue(cached, new TypeReference<List<MasteryVO>>() { });
                }
            } catch (Exception e) {
                log.warn("读今日待练缓存失败，改查数据库: {}", e.getMessage());
            }
        }

        QueryWrapper<Mastery> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.le("next_review", LocalDate.now());
        wrapper.orderByAsc("next_review");

        List<MasteryVO> result = new ArrayList<>();
        for (Mastery mastery : masteryMapper.selectList(wrapper)) {
            result.add(toVO(mastery));
        }

        if (cacheEnabled && !result.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result),
                        Duration.ofSeconds(dueTtlSeconds));
            } catch (Exception e) {
                log.warn("写今日待练缓存失败: {}", e.getMessage());
            }
        }
        return result;
    }

    private String dueKey(Long userId) {
        return "cmmr:due:" + userId;
    }

    private void evictDueCache(Long userId) {
        if (!cacheEnabled) {
            return;
        }
        try {
            redisTemplate.delete(dueKey(userId));
        } catch (Exception e) {
            log.warn("清今日待练缓存失败: {}", e.getMessage());
        }
    }

    @Override
    public List<MasteryVO> weak(Long userId) {
        QueryWrapper<Mastery> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.lt("accuracy", 0.6);
        wrapper.orderByAsc("accuracy");

        List<MasteryVO> result = new ArrayList<>();
        for (Mastery mastery : masteryMapper.selectList(wrapper)) {
            result.add(toVO(mastery));
        }
        return result;
    }
    @Override
    public List<MasteryVO> transfer(Long userId) {
        QueryWrapper<Mastery> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("transfer_flag", 1);
        wrapper.orderByAsc("knowledge_id");

        List<MasteryVO> result = new ArrayList<>();
        for (Mastery mastery : masteryMapper.selectList(wrapper)) {
            result.add(toVO(mastery));
        }
        return result;
    }
@Override
    public CoverageVO coverage(Long userId, Long courseId) {
        long total = 0;
        try {
            Result<Long> result = contentClient.countKnowledge(courseId, userId);
            if (result != null && result.getData() != null) {
                total = result.getData();
            }
        } catch (Exception e) {
            // 内容服务不可用时覆盖率退化成"练过多少"，不让整个看板挂掉
            log.warn("调内容服务取知识点总数失败，覆盖率按 0 计算: {}", e.getMessage());
        }

        QueryWrapper<Mastery> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("course_id", courseId);

        long practiced = 0;
        long mastered = 0;
        for (Mastery mastery : masteryMapper.selectList(wrapper)) {
            practiced++;
            if (mastery.getAccuracy() >= MASTERED_ACCURACY && mastery.getTotalCount() >= MASTERED_MIN_COUNT) {
                mastered++;
            }
        }

        CoverageVO vo = new CoverageVO();
        vo.setTotalKnowledge(total);
        vo.setPracticed(practiced);
        vo.setMastered(mastered);
        vo.setCoverage(total == 0 ? 0.0 : (double) mastered / total);
        return vo;
    }

    @Override
    public Map<String, Object> summary(Long userId) {
        QueryWrapper<Mastery> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);

        long practiced = 0;
        long mastered = 0;
        long dueToday = 0;
        LocalDate today = LocalDate.now();
        for (Mastery mastery : masteryMapper.selectList(wrapper)) {
            practiced++;
            if (mastery.getAccuracy() >= MASTERED_ACCURACY && mastery.getTotalCount() >= MASTERED_MIN_COUNT) {
                mastered++;
            }
            if (mastery.getNextReview() != null && !mastery.getNextReview().isAfter(today)) {
                dueToday++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("practiced", practiced);
        result.put("mastered", mastered);
        result.put("dueToday", dueToday);
        return result;
    }

    @Override
    public void deleteByKnowledge(Long userId, Long knowledgeId) {
        QueryWrapper<Mastery> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("knowledge_id", knowledgeId);
        masteryMapper.delete(wrapper);
        evictDueCache(userId);
    }

    private Mastery findByUserAndKnowledge(Long userId, Long knowledgeId) {
        QueryWrapper<Mastery> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("knowledge_id", knowledgeId);
        return masteryMapper.selectOne(wrapper);
    }

    private MasteryVO toVO(Mastery mastery) {
        MasteryVO vo = new MasteryVO();
        vo.setKnowledgeId(mastery.getKnowledgeId());
        vo.setTotalCount(mastery.getTotalCount());
        vo.setCorrectCount(mastery.getCorrectCount());
        vo.setAccuracy(mastery.getAccuracy());
        vo.setStage(mastery.getStage());
        vo.setNextReview(mastery.getNextReview());
        vo.setSurfaceAccuracy(mastery.getSurfaceAccuracy());
        vo.setDeepAccuracy(mastery.getDeepAccuracy());
        vo.setTransferFlag(mastery.getTransferFlag());
        return vo;
    }
}
