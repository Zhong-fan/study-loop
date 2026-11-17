package com.studyloop.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.studyloop.common.context.UserContext;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.result.Result;
import com.studyloop.practice.client.AttemptPayload;
import com.studyloop.practice.client.InsightClient;
import com.studyloop.practice.client.MasteryDue;
import com.studyloop.practice.entity.Attempt;
import com.studyloop.practice.entity.Question;
import com.studyloop.practice.judge.AnswerJudge;
import com.studyloop.practice.mapper.AttemptMapper;
import com.studyloop.practice.mapper.QuestionMapper;
import com.studyloop.practice.service.ReviewService;
import com.studyloop.practice.util.JsonConverter;
import com.studyloop.practice.vo.AnswerResultVO;
import com.studyloop.practice.vo.DueQuestionVO;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private final InsightClient insightClient;

    public ReviewServiceImpl(QuestionMapper questionMapper,
                             AttemptMapper attemptMapper,
                             InsightClient insightClient) {
        this.questionMapper = questionMapper;
        this.attemptMapper = attemptMapper;
        this.insightClient = insightClient;
    }

    @Override
    public List<DueQuestionVO> dueQuestions(int limit) {
        Long userId = UserContext.requireUserId();
        int size = limit <= 0 ? 20 : Math.min(limit, MAX_LIMIT);

        List<Long> knowledgeIds;
        try {
            Result<List<MasteryDue>> response = insightClient.due(userId);
            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                return new ArrayList<>();
            }
            knowledgeIds = response.getData().stream()
                    .map(MasteryDue::getKnowledgeId)
                    .distinct()
                    .toList();
        } catch (Exception e) {
            // 覆盖服务不可用时返回空列表，不要在练习服务里硬编码一套排期
            LOG.warn("取今日待练失败: {}", e.getMessage());
            return new ArrayList<>();
        }

        QueryWrapper<Question> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("deleted", 0);
        wrapper.in("knowledge_id", knowledgeIds);
        wrapper.orderByAsc("knowledge_id");
        wrapper.last("limit " + size);

        List<DueQuestionVO> result = new ArrayList<>();
        for (Question question : questionMapper.selectList(wrapper)) {
            DueQuestionVO vo = new DueQuestionVO();
            vo.setQuestionId(question.getId());
            vo.setKnowledgeId(question.getKnowledgeId());
            vo.setType(question.getType());
            vo.setStem(question.getStem());
            vo.setOptions(JsonConverter.toList(question.getOptions()));
            vo.setDifficulty(question.getDifficulty());
            result.add(vo);
        }
        return result;
    }

    @Override
    @GlobalTransactional
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
    @GlobalTransactional
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

        LocalDate next = LocalDate.now().plusDays(1);
        if (question.getKnowledgeId() != null) {
            // 这里不吞异常：覆盖服务失败就整体回滚，
            // 否则 attempt 写进去了、掌握度没更新，账就对不上。
            Result<MasteryDue> response = insightClient.record(
                    question.getUserId(),
                    new AttemptPayload(question.getKnowledgeId(), question.getCourseId(), rating >= 1));
            if (response != null && response.getData() != null && response.getData().getNextReview() != null) {
                next = response.getData().getNextReview();
            }
        }

        AnswerResultVO result = new AnswerResultVO();
        result.setScore(score);
        result.setRating(rating);
        result.setNextDueDate(next);
        result.setCorrectAnswer(JsonConverter.toList(question.getAnswer()));
        result.setAnalysis(question.getAnalysis());
        return result;
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
