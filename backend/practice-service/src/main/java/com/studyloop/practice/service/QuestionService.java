package com.studyloop.practice.service;

import com.studyloop.common.result.PageResult;
import com.studyloop.practice.dto.QuestionCreateRequest;
import com.studyloop.practice.dto.QuestionUpdateRequest;
import com.studyloop.practice.vo.QuestionVO;

/**
 * 题目业务接口。
 */
public interface QuestionService {

    /** 只校验不落库，供批量导入先做整批预检 */
    void validateForSave(QuestionCreateRequest request);

    Long create(QuestionCreateRequest request);

    void update(Long id, QuestionUpdateRequest request);

    void delete(Long id);

    QuestionVO get(Long id);

    PageResult<QuestionVO> page(long page, long size, Long courseId, String type, Integer status);

    /** 错题本：最后一次作答未满分的题 */
    java.util.List<QuestionVO> wrongBook();
}
