package com.studyloop.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.studyloop.common.context.UserContext;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.result.Result;
import com.studyloop.practice.client.AttemptPayload;
import com.studyloop.practice.client.InsightClient;
import com.studyloop.practice.client.MasteryDue;
import com.studyloop.practice.entity.Attempt;
import com.studyloop.practice.entity.AttemptDetail;
import com.studyloop.practice.entity.Question;
import com.studyloop.practice.entity.QuestionKnowledge;
import com.studyloop.practice.judge.AnswerJudge;
import com.studyloop.practice.mapper.AttemptDetailMapper;
import com.studyloop.practice.mapper.AttemptMapper;
import com.studyloop.practice.mapper.QuestionKnowledgeMapper;
import com.studyloop.practice.mapper.QuestionMapper;
import com.studyloop.practice.service.ReviewService;
import com.studyloop.practice.util.JsonConverter;
import com.studyloop.practice.vo.AnswerResultVO;
import com.studyloop.practice.vo.DueQueueVO;
import com.studyloop.practice.vo.DueQuestionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 练习业务。
 * <p>
 * 一次作答 = 写 attempt 流水 + 回调 insight 更新掌握度。
 * 下次复习时间取 insight 的返回值，练习服务不再自己算间隔。
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    /** 单次最多取多少道，防止一口气给人 500 道 */
    private static final int MAX_LIMIT = 200;

    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final QuestionMapper questionMapper;
    private final AttemptMapper attemptMapper;
    private final AttemptDetailMapper attemptDetailMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final InsightClient insightClient;

    public ReviewServiceImpl(QuestionMapper questionMapper,
                             AttemptMapper attemptMapper,
                             AttemptDetailMapper attemptDetailMapper,
                             QuestionKnowledgeMapper questionKnowledgeMapper,
                             InsightClient insightClient) {
        this.questionMapper = questionMapper;
        this.attemptMapper = attemptMapper;
        this.attemptDetailMapper = attemptDetailMapper;
        this.questionKnowledgeMapper = questionKnowledgeMapper;
        this.insightClient = insightClient;
    }

    @Override
    public DueQueueVO dueQuestions(int limit) {
        Long userId = UserContext.requireUserId();
        int size = limit <= 0 ? 20 : Math.min(limit, MAX_LIMIT);
        DueQueueVO queue = new DueQueueVO();

        List<Long> knowledgeIds;
        try {
            Result<List<MasteryDue>> response = insightClient.due(userId);
            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                return queue;
            }
            knowledgeIds = response.getData().stream()
                    .map(MasteryDue::getKnowledgeId)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            // 覆盖服务不可用时返回空队列，不要在练习服务里硬编码一套排期
            LOG.warn("取今日待练失败: {}", e.getMessage());
            return queue;
        }

        // 多知识点题目：主知识点之外，关联知识点也可能到期，
        // 所以先把 question_knowledge 里命中的题目一并捞出来
        Set<Long> linkedQuestionIds = questionKnowledgeMapper.selectList(
                        new QueryWrapper<QuestionKnowledge>().in("knowledge_id", knowledgeIds))
                .stream()
                .map(QuestionKnowledge::getQuestionId)
                .collect(java.util.stream.Collectors.toSet());

        QueryWrapper<Question> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("deleted", 0);
        if (linkedQuestionIds.isEmpty()) {
            wrapper.in("knowledge_id", knowledgeIds);
        } else {
            wrapper.and(w -> w.in("id", linkedQuestionIds).or().in("knowledge_id", knowledgeIds));
        }
        wrapper.orderByAsc("knowledge_id");
        wrapper.last("limit " + size);

        Set<Long> withQuestion = new HashSet<>();
        for (Question question : questionMapper.selectList(wrapper)) {
            DueQuestionVO vo = new DueQuestionVO();
            vo.setQuestionId(question.getId());
            vo.setKnowledgeId(question.getKnowledgeId());
            vo.setType(question.getType());
            vo.setStem(question.getStem());
            vo.setOptions(JsonConverter.toList(question.getOptions()));
            vo.setDifficulty(question.getDifficulty());
            queue.getQuestions().add(vo);
            withQuestion.add(question.getKnowledgeId());
        }

        for (Long knowledgeId : knowledgeIds) {
            if (!withQuestion.contains(knowledgeId)) {
                queue.getPendingKnowledgeIds().add(knowledgeId);
            }
        }
        return queue;
    }

    @Override
    @Transactional
    public AnswerResultVO answerQuestion(Long questionId, List<String> answer, Long costMs) {
        Question question = findOwned(questionId);
        if (!AnswerJudge.autoGradable(question.getType())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该题型不支持自动判分，请使用自评接口");
        }

        List<String> correct = JsonConverter.toList(question.getAnswer());
        double score = AnswerJudge.judge(question.getType(), correct, answer);

        int rating;
        if (score >= 1.0) {
            rating = 2;
        } else if (score > 0) {
            rating = 1;
        } else {
            rating = 0;
        }
        return applyResult(question, rating, score, costMs == null ? 0L : costMs, "AUTO");
    }

    @Override
    @Transactional
    public AnswerResultVO rateQuestion(Long questionId, int rating, Long costMs) {
        Question question = findOwned(questionId);
        double score = rating == 2 ? 1.0 : (rating == 1 ? 0.5 : 0);
        return applyResult(question, rating, score, costMs == null ? 0L : costMs, "SELF");
    }

    /**
     * 写流水 → 回调覆盖服务 → 用返回的下次复习日期组装结果。
     */
    private AnswerResultVO applyResult(Question question, int rating, double score,
                                       Long costMs, String grader) {
        Attempt attempt = new Attempt();
        attempt.setUserId(question.getUserId());
        attempt.setQuestionId(question.getId());
        attempt.setKnowledgeId(question.getKnowledgeId());
        attempt.setScore(score);
        attempt.setGrader(grader);
        attempt.setCostMs(costMs);
        attempt.setAnsweredAt(LocalDateTime.now());
        attemptMapper.insert(attempt);

        // 把这道题的得分拆到每个知识点：单点题记 surface，多知识点题记 deep
        List<QuestionKnowledge> links = questionKnowledgeMapper.selectList(
                new QueryWrapper<QuestionKnowledge>().eq("question_id", question.getId()));
        List<AttemptPayload.Detail> details = buildDetails(question, links, score);
        for (AttemptPayload.Detail detail : details) {
            AttemptDetail row = new AttemptDetail();
            row.setAttemptId(attempt.getId());
            row.setKnowledgeId(detail.knowledgeId());
            row.setQuestionType(question.getType());
            row.setContext(detail.context());
            row.setWeight(detail.weight());
            row.setScore(detail.score());
            attemptDetailMapper.insert(row);
        }

        // attempt 已经是事实；掌握度是投影，失败只标记，下次重试
        LocalDate next = LocalDate.now().plusDays(1);
        boolean synced = true;
        if (!details.isEmpty()) {
            SyncOutcome outcome = pushMastery(question.getUserId(), attempt,
                    new AttemptPayload(attempt.getId(), question.getKnowledgeId(),
                            question.getCourseId(), rating >= 1, details));
            synced = outcome.ok();
            if (outcome.ok() && outcome.due() != null && outcome.due().getNextReview() != null) {
                next = outcome.due().getNextReview();
            }
        }
        markSync(attempt.getId(), synced, synced ? null : "投影服务暂不可用");
        attempt.setSynced(synced ? 1 : 0);

        AnswerResultVO result = new AnswerResultVO();
        result.setScore(score);
        result.setRating(rating);
        result.setNextDueDate(next);
        result.setMasterySynced(synced);
        result.setCorrectAnswer(JsonConverter.toList(question.getAnswer()));
        result.setAnalysis(question.getAnalysis());
        return result;
    }

    /**
     * 把作答事件推给覆盖服务。
     * <p>
     * 注意：code == 0 就代表服务端已受理——包括「这个 attempt 已经投影过」的幂等重复，
     * 此时 data 可能为 null，不能当作失败，否则会永远重试。
     */
    private SyncOutcome pushMastery(Long userId, Attempt attempt, AttemptPayload payload) {
        try {
            Result<MasteryDue> response = insightClient.record(userId, payload);
            if (response != null && response.getCode() == 0) {
                return new SyncOutcome(true, response.getData());
            }
            LOG.warn("掌握度投影失败 attempt={} code={}", attempt.getId(),
                    response == null ? "null" : response.getCode());
        } catch (Exception e) {
            LOG.warn("掌握度投影异常 attempt={}: {}", attempt.getId(), e.getMessage());
        }
        return new SyncOutcome(false, null);
    }

    /** ok=服务端已受理（含幂等重复），due 可能为 null */
    private record SyncOutcome(boolean ok, MasteryDue due) {
    }

    private void markSync(Long attemptId, boolean synced, String error) {
        UpdateWrapper<Attempt> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", attemptId);
        wrapper.set("synced", synced ? 1 : 0);
        if (error == null) {
            wrapper.set("last_sync_error", null);
        } else {
            wrapper.set("last_sync_error", error.length() > 190 ? error.substring(0, 190) : error);
        }
        attemptMapper.update(null, wrapper);
    }

    @Override
    public int retryUnsynced(int limit) {
        List<Attempt> pending = attemptMapper.selectList(new QueryWrapper<Attempt>()
                .eq("synced", 0)
                .lt("sync_attempts", 10)
                .orderByAsc("id")
                .last("limit " + Math.max(1, Math.min(limit, 200))));
        int success = 0;
        for (Attempt attempt : pending) {
            Question question = questionMapper.selectById(attempt.getQuestionId());
            if (question == null) {
                continue;
            }
            List<AttemptDetail> rows = attemptDetailMapper.selectList(
                    new QueryWrapper<AttemptDetail>().eq("attempt_id", attempt.getId()));
            List<AttemptPayload.Detail> details = new ArrayList<>();
            for (AttemptDetail row : rows) {
                details.add(new AttemptPayload.Detail(row.getKnowledgeId(), row.getContext(),
                        row.getScore(), row.getWeight()));
            }
            if (details.isEmpty()) {
                continue;
            }
            SyncOutcome outcome = pushMastery(attempt.getUserId(), attempt,
                    new AttemptPayload(attempt.getId(), attempt.getKnowledgeId(),
                            question.getCourseId(), attempt.getScore() != null && attempt.getScore() >= 1,
                            details));
            if (outcome.ok()) {
                markSync(attempt.getId(), true, null);
                success++;
            } else {
                int nextAttempts = (attempt.getSyncAttempts() == null ? 0 : attempt.getSyncAttempts()) + 1;
                UpdateWrapper<Attempt> wrapper = new UpdateWrapper<>();
                wrapper.eq("id", attempt.getId());
                wrapper.setSql("sync_attempts = sync_attempts + 1");
                if (nextAttempts >= 10) {
                    wrapper.set("last_sync_error", "超过重试上限，需人工介入");
                    LOG.error("掌握度补投失败超过上限 attempt={}，需要人工处理", attempt.getId());
                } else {
                    wrapper.set("last_sync_error", "重试失败");
                }
                attemptMapper.update(null, wrapper);
            }
        }
        return success;
    }

    /**
     * 一道题的得分拆到知识点。
     * 只挂一个知识点算单点情境，挂多个（或题型是综合题）算综合情境。
     */
    private List<AttemptPayload.Detail> buildDetails(Question question, List<QuestionKnowledge> links, double score) {
        List<AttemptPayload.Detail> details = new ArrayList<>();
        if (links != null && !links.isEmpty()) {
            boolean composite = links.size() > 1 || "COMPOSITE".equalsIgnoreCase(question.getType());
            for (QuestionKnowledge link : links) {
                details.add(new AttemptPayload.Detail(link.getKnowledgeId(),
                        composite ? "COMPOSITE" : "SINGLE",
                        score,
                        link.getWeight() == null ? 1.0 : link.getWeight()));
            }
        } else if (question.getKnowledgeId() != null) {
            // 老数据没有关联表，退回单知识点
            details.add(new AttemptPayload.Detail(question.getKnowledgeId(), "SINGLE", score, 1.0));
        }
        return details;
    }

    private Question findOwned(Long questionId) {
        Long userId = UserContext.requireUserId();

        Question question = questionMapper.selectById(questionId);
        if (question == null || question.getDeleted() == 1 || !userId.equals(question.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return question;
    }
}
