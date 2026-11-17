package com.studyloop.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyloop.practice.entity.ReviewLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 复习流水表的数据访问。
 */
@Mapper
public interface ReviewLogMapper extends BaseMapper<ReviewLog> {
}
