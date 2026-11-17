package com.studyloop.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyloop.practice.entity.QuizItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuizItemMapper extends BaseMapper<QuizItem> {
}
