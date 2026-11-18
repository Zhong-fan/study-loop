package com.studyloop.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyloop.practice.entity.Quiz;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuizMapper extends BaseMapper<Quiz> {
}
