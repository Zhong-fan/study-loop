package com.studyloop.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.studyloop.common.context.UserContext;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.review.dto.QuizCreateRequest;
import com.studyloop.review.dto.QuizSubmitRequest;
import com.studyloop.review.entity.Question;
import com.studyloop.review.entity.Quiz;
import com.studyloop.review.entity.QuizAnswer;
import com.studyloop.review.entity.QuizItem;
import com.studyloop.review.entity.ReviewCard;
import com.studyloop.review.judge.AnswerJudge;
import com.studyloop.review.mapper.QuestionMapper;
import com.studyloop.review.mapper.QuizAnswerMapper;
import com.studyloop.review.mapper.QuizItemMapper;
import com.studyloop.review.mapper.QuizMapper;
import com.studyloop.review.mapper.ReviewCardMapper;
import com.studyloop.review.service.QuizService;
import com.studyloop.review.service.ReviewService;
import com.studyloop.review.util.JsonConverter;
import com.studyloop.review.vo.QuizDetailVO;
import com.studyloop.review.vo.QuizQuestionVO;
import com.studyloop.review.vo.QuizResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组卷与交卷判分。
 * <p>
 * 两个要点：
 * 1. 只抽客观题 —— 主观题不能自动判分，混进来就没有"交卷即出分"。
 * 2. 判完分把结果回流到复习卡，让这次自测影响下次复习时间。
 */
@Slf4j
@Service
public class QuizServiceImpl implements QuizService {

    /** 能参与组卷的题型 */
    private static final List<String> OBJECTIVE_TYPES = Arrays.asList(
            "SINGLE", "MULTIPLE", "JUDGE", "BLANK"
    );

    private final QuizMapper quizMapper;
    private final QuizItemMapper quizItemMapper;
    private final QuizAnswerMapper quizAnswerMapper;
    private final QuestionMapper questionMapper;
    private final ReviewCardMapper reviewCardMapper;
    private final ReviewService reviewService;

    public QuizServiceImpl(QuizMapper quizMapper,
                           QuizItemMapper quizItemMapper,
                           QuizAnswerMapper quizAnswerMapper,
                           QuestionMapper questionMapper,
                           ReviewCardMapper reviewCardMapper,
                           ReviewService reviewService) {
        this.quizMapper = quizMapper;
        this.quizItemMapper = quizItemMapper;
        this.quizAnswerMapper = quizAnswerMapper;
        this.questionMapper = questionMapper;
        this.reviewCardMapper = reviewCardMapper;
        this.reviewService = reviewService;
    }

    @Override
    @Transactional
    public Long create(QuizCreateRequest request) {
        Long userId = UserContext.requireUserId();

        // RAND() 在几百条数据上完全够用；数据量大了再换成"先取 id 再抽样"
        QueryWrapper<Question> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("deleted", 0);
        wrapper.eq("course_id", request.getCourseId());
        wrapper.in("type", OBJECTIVE_TYPES);
        wrapper.last("ORDER BY RAND() LIMIT " + request.getCount());

        List<Question> questions = questionMapper.selectList(wrapper);
        if (questions.isEmpty()) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "这门课还没有可以组卷的客观题");
        }

        Quiz quiz = new Quiz();
        quiz.setUserId(userId);
        quiz.setCourseId(request.getCourseId());
        quiz.setTitle(request.getTitle() == null || request.getTitle().isBlank()
                ? "自测 " + LocalDateTime.now().toLocalDate()
                : request.getTitle().trim());
        quiz.setQuestionCount(questions.size());
        quiz.setStatus(0);
        quiz.setCreatedAt(LocalDateTime.now());
        quizMapper.insert(quiz);

        int sort = 1;
        for (Question question : questions) {
            QuizItem item = new QuizItem();
            item.setQuizId(quiz.getId());
            item.setQuestionId(question.getId());
            item.setSort(sort++);
            quizItemMapper.insert(item);
        }
        return quiz.getId();
    }

    @Override
    public QuizDetailVO detail(Long quizId) {
        Quiz quiz = findOwnedQuiz(quizId);
        List<QuizItem> items = listItems(quizId);

        List<Long> ids = new ArrayList<>();
        for (QuizItem item : items) {
            ids.add(item.getQuestionId());
        }
        Map<Long, Question> questionMap = loadQuestions(ids);

        QuizDetailVO vo = new QuizDetailVO();
        vo.setId(quiz.getId());
        vo.setTitle(quiz.getTitle());
        vo.setStatus(quiz.getStatus());

        List<QuizQuestionVO> questions = new ArrayList<>();
        for (QuizItem item : items) {
            Question question = questionMap.get(item.getQuestionId());
            if (question == null) {
                continue;
            }
            QuizQuestionVO q = new QuizQuestionVO();
            q.setQuestionId(question.getId());
            q.setType(question.getType());
            q.setStem(question.getStem());
            q.setOptions(JsonConverter.toList(question.getOptions()));
            q.setDifficulty(question.getDifficulty());
            q.setSort(item.getSort());
            questions.add(q);
        }
        vo.setQuestions(questions);
        return vo;
    }

    @Override
    @Transactional
    public QuizResultVO submit(Long quizId, QuizSubmitRequest request) {
        Quiz quiz = findOwnedQuiz(quizId);
        if (quiz.getStatus() == 1) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "这份卷子已经交过了");
        }

        List<QuizItem> items = listItems(quizId);
        Map<Long, Question> questionMap = loadQuestions(collectIds(items));

        Map<Long, List<String>> submitted = new HashMap<>();
        if (request.getAnswers() != null) {
            for (QuizSubmitRequest.Item item : request.getAnswers()) {
                submitted.put(item.getQuestionId(), item.getAnswer());
            }
        }

        double scoreSum = 0;
        int correctCount = 0;
        List<QuizResultVO.Item> resultItems = new ArrayList<>();

        for (QuizItem item : items) {
            Question question = questionMap.get(item.getQuestionId());
            if (question == null) {
                continue;
            }

            List<String> correct = JsonConverter.toList(question.getAnswer());
            List<String> yours = submitted.getOrDefault(item.getQuestionId(), new ArrayList<>());
            double score = yours.isEmpty() ? 0 : AnswerJudge.judge(question.getType(), correct, yours);

            scoreSum += score;
            if (score >= 1.0) {
                correctCount++;
            }

            QuizAnswer answer = new QuizAnswer();
            answer.setQuizId(quizId);
            answer.setQuestionId(question.getId());
            answer.setUserAnswer(JsonConverter.toJson(yours));
            answer.setScore(score);
            answer.setCorrect(score >= 1.0 ? 1 : 0);
            answer.setCreatedAt(LocalDateTime.now());
            quizAnswerMapper.insert(answer);

            // 回流：这次答得怎么样，直接决定这张卡下次什么时候复习
            applyToReviewCard(question.getId(), score);

            QuizResultVO.Item resultItem = new QuizResultVO.Item();
            resultItem.setQuestionId(question.getId());
            resultItem.setType(question.getType());
            resultItem.setStem(question.getStem());
            resultItem.setYourAnswer(yours);
            resultItem.setCorrectAnswer(correct);
            resultItem.setScore(score);
            resultItem.setAnalysis(question.getAnalysis());
            resultItems.add(resultItem);
        }

        double total = items.isEmpty() ? 0 : scoreSum / items.size() * 100;
        quiz.setStatus(1);
        quiz.setScore(Math.round(total * 10) / 10.0);
        quiz.setSubmittedAt(LocalDateTime.now());
        quizMapper.updateById(quiz);

        QuizResultVO vo = new QuizResultVO();
        vo.setQuizId(quizId);
        vo.setScore(quiz.getScore());
        vo.setTotal(items.size());
        vo.setCorrectCount(correctCount);
        vo.setItems(resultItems);
        return vo;
    }

    /** 把一道题的得分换算成复习评分，交给复习服务统一排期 */
    private void applyToReviewCard(Long questionId, double score) {
        Long userId = UserContext.requireUserId();

        QueryWrapper<ReviewCard> wrapper = new QueryWrapper<>();
        wrapper.eq("question_id", questionId);
        wrapper.eq("user_id", userId);
        ReviewCard card = reviewCardMapper.selectOne(wrapper);
        if (card == null) {
            return;
        }

        int rating = score >= 1.0 ? 2 : (score > 0 ? 1 : 0);
        try {
            reviewService.rate(card.getId(), rating, 0L);
        } catch (Exception e) {
            // 单张卡排期失败不该让整份卷子交不上
            log.warn("自测回流复习卡失败，cardId={}", card.getId(), e);
        }
    }

    private Quiz findOwnedQuiz(Long quizId) {
        Long userId = UserContext.requireUserId();

        Quiz quiz = quizMapper.selectById(quizId);
        if (quiz == null || !userId.equals(quiz.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "试卷不存在");
        }
        return quiz;
    }

    private List<QuizItem> listItems(Long quizId) {
        QueryWrapper<QuizItem> wrapper = new QueryWrapper<>();
        wrapper.eq("quiz_id", quizId);
        wrapper.orderByAsc("sort");
        return quizItemMapper.selectList(wrapper);
    }

    private List<Long> collectIds(List<QuizItem> items) {
        List<Long> ids = new ArrayList<>();
        for (QuizItem item : items) {
            ids.add(item.getQuestionId());
        }
        return ids;
    }

    private Map<Long, Question> loadQuestions(List<Long> ids) {
        Map<Long, Question> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        // 一次查完，不在循环里查库
        for (Question question : questionMapper.selectBatchIds(ids)) {
            map.put(question.getId(), question);
        }
        return map;
    }
}
