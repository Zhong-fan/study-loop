package com.studyloop.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.studyloop.common.context.UserContext;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.content.dto.ChapterRequest;
import com.studyloop.content.entity.Chapter;
import com.studyloop.content.entity.Course;
import com.studyloop.content.mapper.ChapterMapper;
import com.studyloop.content.mapper.CourseMapper;
import com.studyloop.content.service.ChapterService;
import com.studyloop.content.vo.ChapterVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 章节业务实现。
 * <p>
 * 章节没有独立的权限体系，它跟着课程走：
 * 课程是谁的，章节就是谁的。所以每次操作前都先确认课程归属。
 */
@Service
public class ChapterServiceImpl implements ChapterService {

    private final ChapterMapper chapterMapper;
    private final CourseMapper courseMapper;

    public ChapterServiceImpl(ChapterMapper chapterMapper, CourseMapper courseMapper) {
        this.chapterMapper = chapterMapper;
        this.courseMapper = courseMapper;
    }

    @Override
    public List<ChapterVO> listByCourse(Long courseId) {
        checkCourse(courseId);

        QueryWrapper<Chapter> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId);
        wrapper.eq("deleted", 0);
        wrapper.orderByAsc("sort").orderByAsc("id");

        List<Chapter> chapters = chapterMapper.selectList(wrapper);
        List<ChapterVO> result = new ArrayList<>();
        for (Chapter chapter : chapters) {
            result.add(toVO(chapter));
        }
        return result;
    }

    @Override
    public Long create(Long courseId, ChapterRequest request) {
        Course course = checkCourse(courseId);

        Chapter chapter = new Chapter();
        chapter.setCourseId(courseId);
        chapter.setUserId(course.getUserId());
        chapter.setTitle(request.getTitle().trim());
        chapter.setSort(request.getSort() == null ? nextSort(courseId) : request.getSort());
        chapter.setCreatedAt(LocalDateTime.now());
        chapter.setDeleted(0);

        chapterMapper.insert(chapter);
        return chapter.getId();
    }

    @Override
    public void update(Long id, ChapterRequest request) {
        Chapter chapter = findOwnedChapter(id);

        chapter.setTitle(request.getTitle().trim());
        if (request.getSort() != null) {
            chapter.setSort(request.getSort());
        }
        chapterMapper.updateById(chapter);
    }

    @Override
    public void delete(Long id) {
        Chapter chapter = findOwnedChapter(id);
        chapter.setDeleted(1);
        chapterMapper.updateById(chapter);
    }

    /** 新章节默认排在最后 */
    private int nextSort(Long courseId) {
        QueryWrapper<Chapter> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId);
        wrapper.eq("deleted", 0);
        Long count = chapterMapper.selectCount(wrapper);
        return count.intValue() + 1;
    }

    /** 确认课程存在且属于当前用户，返回课程本身 */
    private Course checkCourse(Long courseId) {
        Long userId = UserContext.requireUserId();

        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getDeleted() == 1 || !userId.equals(course.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
        return course;
    }

    private Chapter findOwnedChapter(Long id) {
        Long userId = UserContext.requireUserId();

        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null || chapter.getDeleted() == 1 || !userId.equals(chapter.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "章节不存在");
        }
        return chapter;
    }

    private ChapterVO toVO(Chapter chapter) {
        ChapterVO vo = new ChapterVO();
        vo.setId(chapter.getId());
        vo.setCourseId(chapter.getCourseId());
        vo.setTitle(chapter.getTitle());
        vo.setSort(chapter.getSort());
        return vo;
    }
}
