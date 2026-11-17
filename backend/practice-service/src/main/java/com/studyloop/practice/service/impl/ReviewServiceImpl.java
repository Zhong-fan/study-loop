package com.studyloop.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyloop.common.context.UserContext;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.practice.entity.Question;
import com.studyloop.practice.entity.ReviewCard;
import com.studyloop.practice.entity.ReviewLog;
import com.studyloop.practice.judge.AnswerJudge;
import com.studyloop.practice.judge.Sm2Scheduler;
import com.studyloop.practice.mapper.QuestionMapper;
import com.studyloop.practice.mapper.ReviewCardMapper;
import com.studyloop.practice.mapper.ReviewLogMapper;
import com.studyloop.practice.service.ReviewService;
import com.studyloop.practice.util.JsonConverter;
import com.studyloop.practice.vo.AnswerResultVO;
import com.studyloop.practice.vo.ReviewCardVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 复习业务实现。
 * <p>
 * 一次复习 = 改卡片 + 写流水，两件事必须在一个事务里：
 * 只改了卡片没写流水，统计会少一次；只写流水没改卡片，这张卡明天还会出现。
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    /** 单次最多取多少张卡，防止一口气给人 500 道 */
    private static final int MAX_LIMIT = 200;

    private final ReviewCardMapper reviewCardMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final QuestionMapper questionMapper;

    public ReviewServiceImpl(ReviewCardMapper reviewCardMapper,
                             ReviewLogMapper reviewLogMapper,
                             QuestionMapper questionMapper) {
        this.reviewCardMapper = reviewCardMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.questionMapper = questionMapper;
    }

    @Override
    public List<ReviewCardVO> today(int limit) {
        Long userId = UserContext.requireUserId();
        int size = limit <= 0 ? 50 : Math.min(limit, MAX_LIMIT);

        QueryWrapper<ReviewCard> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("deleted", 0);
        wrapper.le("due_date", LocalDate.now());
        wrapper.orderByAsc("due_date");

        List<ReviewCard> cards = reviewCardMapper
                .selectPage(new Page<>(1, size), wrapper)
                .getRecords();
        if (cards.isEmpty()) {
            return new ArrayList<>();
        }

        // 一次把题目都查出来，不要在循环里查库
        List<Long> questionIds = new ArrayList<>();
        for (ReviewCard card : cards) {
            questionIds.add(card.getQuestionId());
        }
        Map<Long, Question> questionMap = new HashMap<>();
        for (Question question : questionMapper.selectBatchIds(questionIds)) {
            questionMap.put(question.getId(), question);
        }

        List<ReviewCardVO> result = new ArrayList<>();
        for (ReviewCard card : cards) {
            Question question = questionMap.get(card.getQuestionId());
            if (question == null) {
                continue;
            }
            result.add(toVO(card, question));
        }
        return result;
    }

    @Override
    @Transactional
    public AnswerResultVO submitAnswer(Long cardId, List<String> answer, Long costMs) {
        ReviewCard card = findOwnedCard(cardId);
        Question question = findQuestion(card.getQuestionId());

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

        AnswerResultVO result = applyRating(card, question, rating, costMs == null ? 0L : costMs);
        result.setScore(score);
        return result;
    }

    @Override
    @Transactional
    public AnswerResultVO rate(Long cardId, int rating, Long costMs) {
        ReviewCard card = findOwnedCard(cardId);
        Question question = findQuestion(card.getQuestionId());
        return applyRating(card, question, rating, costMs == null ? 0L : costMs);
    }

    /**
     * 排期：算新状态 → 更新卡片 → 写流水。
     * 主观题自评和客观题判分最后都走这里，保证只有一套排期逻辑。
     */
    private AnswerResultVO applyRating(ReviewCard card, Question question, int rating, Long costMs) {
        Sm2Scheduler.Result next = Sm2Scheduler.next(
                rating,
                card.getEaseFactor(),
                card.getIntervalDays(),
                card.getRepetitions(),
                card.getLapses()
        );

        ReviewLog log = new ReviewLog();
        log.setCardId(card.getId());
        log.setUserId(card.getUserId());
        log.setRating(rating);
        log.setPrevInterval(card.getIntervalDays());
        log.setNextInterval(next.intervalDays);
        log.setPrevEase(card.getEaseFactor());
        log.setNextEase(next.ease);
        log.setCostMs(costMs);
        log.setReviewedAt(LocalDateTime.now());

        card.setEaseFactor(next.ease);
        card.setIntervalDays(next.intervalDays);
        card.setRepetitions(next.repetitions);
        card.setLapses(next.lapses);
        card.setState("REVIEW");
        card.setLastReviewAt(LocalDateTime.now());
        card.setDueDate(LocalDate.now().plusDays(next.intervalDays));
        reviewCardMapper.updateById(card);

        reviewLogMapper.insert(log);

        // 间隔超过 30 天就算"掌握"，但仍然参与长间隔复习
        if (next.intervalDays >= 30 && question.getStatus() == 0) {
            question.setStatus(1);
            question.setUpdatedAt(LocalDateTime.now());
            questionMapper.updateById(question);
        }

        AnswerResultVO result = new AnswerResultVO();
        result.setScore(rating == 2 ? 1.0 : (rating == 1 ? 0.5 : 0));
        result.setRating(rating);
        result.setNextDueDate(card.getDueDate());
        result.setCorrectAnswer(JsonConverter.toList(question.getAnswer()));
        result.setAnalysis(question.getAnalysis());
        return result;
    }

    private ReviewCard findOwnedCard(Long cardId) {
        Long userId = UserContext.requireUserId();

        ReviewCard card = reviewCardMapper.selectById(cardId);
        if (card == null || card.getDeleted() == 1 || !userId.equals(card.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "复习卡不存在");
        }
        return card;
    }

    private Question findQuestion(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null || question.getDeleted() == 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return question;
    }

    private ReviewCardVO toVO(ReviewCard card, Question question) {
        ReviewCardVO vo = new ReviewCardVO();
        vo.setCardId(card.getId());
        vo.setQuestionId(question.getId());
        vo.setType(question.getType());
        vo.setStem(question.getStem());
        vo.setOptions(JsonConverter.toList(question.getOptions()));
        vo.setDifficulty(question.getDifficulty());
        vo.setState(card.getState());
        vo.setIntervalDays(card.getIntervalDays());
        vo.setRepetitions(card.getRepetitions());
        vo.setDueDate(card.getDueDate());
        return vo;
    }
}
