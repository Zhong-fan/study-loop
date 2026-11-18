package com.studyloop.practice.service;

import com.studyloop.common.result.PageResult;
import com.studyloop.practice.dto.QuestionCreateRequest;
import com.studyloop.practice.dto.QuestionUpdateRequest;
import com.studyloop.practice.vo.QuestionVO;

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
