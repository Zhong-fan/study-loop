package com.studyloop.content.service;

import com.studyloop.content.dto.ChapterRequest;
import com.studyloop.content.vo.ChapterVO;

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
