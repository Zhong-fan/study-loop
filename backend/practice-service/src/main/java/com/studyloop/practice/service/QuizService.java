package com.studyloop.practice.service;

import com.studyloop.practice.dto.QuizCreateRequest;
import com.studyloop.practice.dto.QuizSubmitRequest;
import com.studyloop.practice.vo.QuizDetailVO;
import com.studyloop.practice.vo.QuizResultVO;

/**
 * 组卷与自测。
 */
public interface QuizService {

    Long create(QuizCreateRequest request);

    QuizDetailVO detail(Long quizId);

    QuizResultVO submit(Long quizId, QuizSubmitRequest request);
}
