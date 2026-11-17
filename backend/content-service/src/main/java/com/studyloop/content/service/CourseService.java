package com.studyloop.content.service;

import com.studyloop.common.result.PageResult;
import com.studyloop.content.dto.CourseCreateRequest;
import com.studyloop.content.dto.CourseUpdateRequest;
import com.studyloop.content.vo.CourseVO;

/**
 * 课程业务接口。
 * <p>
 * 先写接口再写实现，是为了让 Controller 只依赖接口，
 * 以后要换实现（比如加缓存）不用改 Controller。
 */
public interface CourseService {

    Long create(CourseCreateRequest request);

    void update(Long id, CourseUpdateRequest request);

    void delete(Long id);

    CourseVO getById(Long id);

    PageResult<CourseVO> page(long pageNo, long pageSize, String keyword);
}
