package com.studyloop.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyloop.common.context.UserContext;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.result.PageResult;
import com.studyloop.practice.dto.QuestionCreateRequest;
import com.studyloop.practice.dto.QuestionUpdateRequest;
import com.studyloop.practice.entity.Question;
import com.studyloop.practice.entity.ReviewCard;
import com.studyloop.practice.mapper.QuestionMapper;
import com.studyloop.practice.mapper.ReviewCardMapper;
import com.studyloop.practice.service.QuestionService;
import com.studyloop.practice.util.JsonConverter;
import com.studyloop.practice.vo.QuestionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 题目业务实现。
 * <p>
 * 两个关键点：
 * 1. 建题和建卡必须在一个事务里 —— 只有题没有卡，这道题永远不会出现在复习列表。
 * 2. 保存前要按题型校验选项和答案，否则脏数据会一直留到判分时才炸。
 */
@Service
public class QuestionServiceImpl implements QuestionService {

    private static final List<String> TYPES = Arrays.asList(
            "SINGLE", "MULTIPLE", "JUDGE", "BLANK", "SUBJECTIVE", "IMAGE"
    );

    private final QuestionMapper questionMapper;
    private final ReviewCardMapper reviewCardMapper;

    public QuestionServiceImpl(QuestionMapper questionMapper, ReviewCardMapper reviewCardMapper) {
        this.questionMapper = questionMapper;
        this.reviewCardMapper = reviewCardMapper;
    }

    @Override
    @Transactional
    public Long create(QuestionCreateRequest request) {
        Long userId = UserContext.requireUserId();
        validate(request.getType(), request.getOptions(), request.getAnswer());

        Question question = new Question();
        question.setUserId(userId);
        question.setCourseId(request.getCourseId());
        question.setChapterId(request.getChapterId());
        question.setKnowledgeId(request.getKnowledgeId());
        question.setMaterialId(request.getMaterialId());
        question.setPageNo(request.getPageNo());
        question.setQuote(request.getQuote());
        question.setType(request.getType());
        question.setStem(request.getStem().trim());
        question.setOptions(JsonConverter.toJson(request.getOptions()));
        question.setAnswer(JsonConverter.toJson(request.getAnswer()));
        question.setAnalysis(request.getAnalysis());
        question.setDifficulty(request.getDifficulty() == null ? 3 : request.getDifficulty());
        question.setStatus(0);
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());
        question.setDeleted(0);
        questionMapper.insert(question);

        ReviewCard card = new ReviewCard();
        card.setUserId(userId);
        card.setQuestionId(question.getId());
        card.setState("NEW");
        card.setEaseFactor(2.5);
        card.setIntervalDays(0);
        card.setRepetitions(0);
        card.setLapses(0);
        card.setDueDate(LocalDate.now());
        card.setCreatedAt(LocalDateTime.now());
        card.setDeleted(0);
        reviewCardMapper.insert(card);

        return question.getId();
    }

    @Override
    public void update(Long id, QuestionUpdateRequest request) {
        Question question = findOwnedQuestion(id);
        validate(request.getType(), request.getOptions(), request.getAnswer());

        question.setChapterId(request.getChapterId());
        question.setType(request.getType());
        question.setStem(request.getStem().trim());
        question.setOptions(JsonConverter.toJson(request.getOptions()));
        question.setAnswer(JsonConverter.toJson(request.getAnswer()));
        question.setAnalysis(request.getAnalysis());
        if (request.getDifficulty() != null) {
            question.setDifficulty(request.getDifficulty());
        }
        question.setUpdatedAt(LocalDateTime.now());
        questionMapper.updateById(question);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Question question = findOwnedQuestion(id);
        question.setDeleted(1);
        question.setUpdatedAt(LocalDateTime.now());
        questionMapper.updateById(question);

        // 卡片一起失效，否则今日复习还会把它捞出来
        QueryWrapper<ReviewCard> wrapper = new QueryWrapper<>();
        wrapper.eq("question_id", id);
        ReviewCard card = reviewCardMapper.selectOne(wrapper);
        if (card != null) {
            card.setDeleted(1);
            reviewCardMapper.updateById(card);
        }
    }

    @Override
    public QuestionVO get(Long id) {
        return toVO(findOwnedQuestion(id));
    }

    @Override
    public PageResult<QuestionVO> page(long pageNo, long pageSize, Long courseId, String type, Integer status) {
        Long userId = UserContext.requireUserId();

        QueryWrapper<Question> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("deleted", 0);
        if (courseId != null) {
            wrapper.eq("course_id", courseId);
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq("type", type);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("created_at");

        Page<Question> result = questionMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);

        List<QuestionVO> records = new ArrayList<>();
        for (Question question : result.getRecords()) {
            records.add(toVO(question));
        }
        return PageResult.of(result.getTotal(), pageNo, pageSize, records);
    }

    /**
     * 按题型校验。规则集中在这里，以后加题型只改这一个方法。
     */
    private void validate(String type, List<String> options, List<String> answer) {
        if (!TYPES.contains(type)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "未知题型：" + type);
        }
        if (answer == null || answer.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "答案不能为空");
        }

        boolean choice = "SINGLE".equals(type) || "MULTIPLE".equals(type);
        if (choice) {
            if (options == null || options.size() < 2) {
                throw new BizException(ErrorCode.PARAM_ERROR, "选择题至少要有 2 个选项");
            }
            for (String item : answer) {
                int index = letterToIndex(item);
                if (index < 0 || index >= options.size()) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "答案 " + item + " 不在选项范围内");
                }
            }
        }
        if ("SINGLE".equals(type) && answer.size() != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "单选题只能有一个答案");
        }
        if ("MULTIPLE".equals(type) && answer.size() < 2) {
            throw new BizException(ErrorCode.PARAM_ERROR, "多选题至少要选两个答案");
        }
    }

    /** "A" -> 0, "B" -> 1；不是合法字母返回 -1 */
    private int letterToIndex(String letter) {
        if (letter == null || letter.length() != 1) {
            return -1;
        }
        char c = Character.toUpperCase(letter.charAt(0));
        if (c < 'A' || c > 'F') {
            return -1;
        }
        return c - 'A';
    }

    private Question findOwnedQuestion(Long id) {
        Long userId = UserContext.requireUserId();

        Question question = questionMapper.selectById(id);
        if (question == null || question.getDeleted() == 1 || !userId.equals(question.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return question;
    }

    private QuestionVO toVO(Question question) {
        QuestionVO vo = new QuestionVO();
        vo.setId(question.getId());
        vo.setCourseId(question.getCourseId());
        vo.setChapterId(question.getChapterId());
        vo.setKnowledgeId(question.getKnowledgeId());
        vo.setMaterialId(question.getMaterialId());
        vo.setPageNo(question.getPageNo());
        vo.setQuote(question.getQuote());
        vo.setType(question.getType());
        vo.setStem(question.getStem());
        vo.setOptions(JsonConverter.toList(question.getOptions()));
        vo.setAnswer(JsonConverter.toList(question.getAnswer()));
        vo.setAnalysis(question.getAnalysis());
        vo.setDifficulty(question.getDifficulty());
        vo.setStatus(question.getStatus());
        vo.setCreatedAt(question.getCreatedAt());
        return vo;
    }
}
