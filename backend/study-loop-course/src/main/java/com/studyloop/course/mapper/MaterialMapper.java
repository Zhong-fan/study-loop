package com.studyloop.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyloop.course.entity.Material;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资料表的数据访问。
 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {
}
