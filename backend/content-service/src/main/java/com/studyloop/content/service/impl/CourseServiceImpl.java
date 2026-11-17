package com.studyloop.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.studyloop.common.context.UserContext;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.result.PageResult;
import com.studyloop.content.dto.CourseCreateRequest;
import com.studyloop.content.dto.CourseUpdateRequest;
import com.studyloop.content.entity.Course;
import com.studyloop.content.mapper.CourseMapper;
import com.studyloop.content.service.CourseService;
import com.studyloop.content.vo.CourseVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程业务实现。
 * <p>
 * 这里只做三件事：拿当前用户、查/存数据库、把 Entity 转成 VO。
 * 没有缓存、没有事件、没有切面 —— 等真的有性能问题再加。
 */
@Service
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;

    public CourseServiceImpl(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    @Override
    public Long create(CourseCreateRequest request) {
        Long userId = UserContext.requireUserId();

        Course course = new Course();
        course.setUserId(userId);
        course.setName(request.getName().trim());
        course.setDescription(request.getDescription());
        course.setColor(request.getColor() == null ? "#409EFF" : request.getColor());
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());
        course.setDeleted(0);

        courseMapper.insert(course);
        return course.getId();
    }

    @Override
    public void update(Long id, CourseUpdateRequest request) {
        Course course = findOwnedCourse(id);

        course.setName(request.getName().trim());
        course.setDescription(request.getDescription());
        if (request.getColor() != null) {
            course.setColor(request.getColor());
        }
        course.setUpdatedAt(LocalDateTime.now());

        courseMapper.updateById(course);
    }

    @Override
    public void delete(Long id) {
        Course course = findOwnedCourse(id);
        // 软删除：只改标记，不真的删行，误删还能恢复
        course.setDeleted(1);
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(course);
    }

    @Override
    public CourseVO getById(Long id) {
        return toVO(findOwnedCourse(id));
    }

    @Override
    public PageResult<CourseVO> page(long pageNo, long pageSize, String keyword) {
        Long userId = UserContext.requireUserId();

        QueryWrapper<Course> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("deleted", 0);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like("name", keyword.trim());
        }
        wrapper.orderByDesc("created_at");

        Page<Course> result = courseMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);

        List<CourseVO> records = new ArrayList<>();
        for (Course course : result.getRecords()) {
            records.add(toVO(course));
        }
        return PageResult.of(result.getTotal(), pageNo, pageSize, records);
    }

    /**
     * 查出课程并确认属于当前用户。
     * 不存在和不是自己的，都按"不存在"处理，避免别人探测 id。
     */
    private Course findOwnedCourse(Long id) {
        Long userId = UserContext.requireUserId();

        Course course = courseMapper.selectById(id);
        if (course == null || course.getDeleted() == 1 || !userId.equals(course.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
        return course;
    }

    /** Entity 转 VO，字段少的时候手写最直观，不需要 MapStruct */
    private CourseVO toVO(Course course) {
        CourseVO vo = new CourseVO();
        vo.setId(course.getId());
        vo.setName(course.getName());
        vo.setDescription(course.getDescription());
        vo.setColor(course.getColor());
        vo.setCreatedAt(course.getCreatedAt());
        return vo;
    }
}
