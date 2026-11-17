package com.studyloop.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyloop.content.entity.Course;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程表的数据访问。
 * <p>
 * 继承 BaseMapper 后，增删改查和分页都由 MyBatis-Plus 提供，
 * 只有写复杂 SQL 时才需要自己加方法（用 @Select 注解或 XML）。
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
