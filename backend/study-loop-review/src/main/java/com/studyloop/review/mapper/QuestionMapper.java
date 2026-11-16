package com.studyloop.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyloop.review.entity.Question;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目表的数据访问。
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
