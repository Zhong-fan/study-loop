package com.studyloop.review.service;

import com.studyloop.common.result.PageResult;
import com.studyloop.review.dto.QuestionCreateRequest;
import com.studyloop.review.dto.QuestionUpdateRequest;
import com.studyloop.review.vo.QuestionVO;

/**
 * 题目业务接口。
 */
public interface QuestionService {

    Long create(QuestionCreateRequest request);

    void update(Long id, QuestionUpdateRequest request);

    void delete(Long id);

    QuestionVO get(Long id);

    PageResult<QuestionVO> page(long page, long size, Long courseId, String type, Integer status);
}
