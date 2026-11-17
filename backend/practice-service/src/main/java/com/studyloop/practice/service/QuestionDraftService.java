package com.studyloop.practice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.practice.dto.QuestionDraftRequest;
import com.studyloop.practice.entity.Question;
import com.studyloop.practice.entity.QuestionKnowledge;
import com.studyloop.practice.entity.QuestionRubric;
import com.studyloop.practice.mapper.QuestionKnowledgeMapper;
import com.studyloop.practice.mapper.QuestionMapper;
import com.studyloop.practice.mapper.QuestionRubricMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 把 AI 出的草稿落库。
 * <p>
 * 一道题会写三张表：question（题干答案）、question_knowledge（挂哪些知识点）、
 * question_rubric（评分点）。三张表在同一个事务里，避免出现"有题没评分点"的半成品。
 */
@Service
public class QuestionDraftService {

    private final QuestionMapper questionMapper;
    private final QuestionKnowledgeMapper knowledgeMapper;
    private final QuestionRubricMapper rubricMapper;
    private final ObjectMapper objectMapper;

    public QuestionDraftService(QuestionMapper questionMapper, QuestionKnowledgeMapper knowledgeMapper,
                                QuestionRubricMapper rubricMapper, ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.knowledgeMapper = knowledgeMapper;
        this.rubricMapper = rubricMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<Long> save(Long userId, Long courseId, Long chapterId, List<QuestionDraftRequest> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "没有要保存的题目");
        }

        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            QuestionDraftRequest draft = drafts.get(i);
            if (draft.getKnowledge() == null || draft.getKnowledge().isEmpty()) {
                throw new BizException(ErrorCode.PARAM_ERROR, "第 " + (i + 1) + " 题没有关联知识点");
            }

            Question question = buildQuestion(userId, courseId, chapterId, draft);
            questionMapper.insert(question);
            ids.add(question.getId());

            for (QuestionDraftRequest.KnowledgeRef ref : draft.getKnowledge()) {
                QuestionKnowledge link = new QuestionKnowledge();
                link.setQuestionId(question.getId());
                link.setKnowledgeId(ref.getKnowledgeId());
                link.setRole(ref.getRole() == null ? "RELATED" : ref.getRole());
                link.setWeight(ref.getWeight() == null ? 0.2 : ref.getWeight());
                knowledgeMapper.insert(link);
            }

            saveRubric(question.getId(), draft);
        }
        return ids;
    }

    private Question buildQuestion(Long userId, Long courseId, Long chapterId, QuestionDraftRequest draft) {
        Question question = new Question();
        question.setUserId(userId);
        question.setCourseId(courseId);
        question.setChapterId(chapterId);
        question.setKnowledgeId(primaryKnowledgeId(draft));
        question.setType(draft.getType());
        question.setStem(draft.getStem());
        question.setOptions(toJson(draft.getOptions()));
        question.setAnswer(toJson(draft.getAnswer()));
        question.setAnalysis(draft.getExplain());
        question.setDifficulty(draft.getDifficulty() == null ? 3 : draft.getDifficulty());
        question.setStatus(1);
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());
        question.setDeleted(0);

        if (draft.getEvidence() != null && !draft.getEvidence().isEmpty()) {
            QuestionDraftRequest.Evidence evidence = draft.getEvidence().get(0);
            question.setMaterialId(evidence.getMaterialId());
            question.setPageNo(evidence.getPage());
            question.setQuote(evidence.getQuote());
        }
        return question;
    }

    private Long primaryKnowledgeId(QuestionDraftRequest draft) {
        return draft.getKnowledge().stream()
                .filter(k -> "PRIMARY".equalsIgnoreCase(k.getRole()))
                .map(QuestionDraftRequest.KnowledgeRef::getKnowledgeId)
                .findFirst()
                .orElse(draft.getKnowledge().get(0).getKnowledgeId());
    }

    private void saveRubric(Long questionId, QuestionDraftRequest draft) {
        if (draft.getSubQuestions() == null) {
            return;
        }
        for (QuestionDraftRequest.SubQuestion sub : draft.getSubQuestions()) {
            if (sub.getRubric() == null) {
                continue;
            }
            for (QuestionDraftRequest.Rubric item : sub.getRubric()) {
                QuestionRubric rubric = new QuestionRubric();
                rubric.setQuestionId(questionId);
                rubric.setSubNo(sub.getNo());
                rubric.setPointText(item.getPoint());
                rubric.setScore(item.getScore());
                rubric.setKnowledgeId(item.getKnowledgeId());
                rubricMapper.insert(rubric);
            }
        }
    }

    private String toJson(List<String> values) {
        if (values == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            return "[]";
        }
    }
}
