package com.studyloop.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyloop.content.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
