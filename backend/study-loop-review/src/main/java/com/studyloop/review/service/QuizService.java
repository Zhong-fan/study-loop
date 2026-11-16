package com.studyloop.review.service;

import com.studyloop.review.dto.QuizCreateRequest;
import com.studyloop.review.dto.QuizSubmitRequest;
import com.studyloop.review.vo.QuizDetailVO;
import com.studyloop.review.vo.QuizResultVO;

/**
 * 组卷与自测。
 */
public interface QuizService {

    Long create(QuizCreateRequest request);

    QuizDetailVO detail(Long quizId);

    QuizResultVO submit(Long quizId, QuizSubmitRequest request);
}
