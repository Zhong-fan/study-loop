package com.studyloop.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyloop.practice.entity.Attempt;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttemptMapper extends BaseMapper<Attempt> {
}
