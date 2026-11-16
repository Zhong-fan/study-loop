package com.studyloop.course.service;

import com.studyloop.course.dto.ChapterRequest;
import com.studyloop.course.vo.ChapterVO;

import java.util.List;

/**
 * 章节业务接口。
 */
public interface ChapterService {

    List<ChapterVO> listByCourse(Long courseId);

    Long create(Long courseId, ChapterRequest request);

    void update(Long id, ChapterRequest request);

    void delete(Long id);
}
