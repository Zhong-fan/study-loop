package com.studyloop.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyloop.content.entity.Chapter;
import org.apache.ibatis.annotations.Mapper;

/**
 * 章节表的数据访问。
 */
@Mapper
public interface ChapterMapper extends BaseMapper<Chapter> {
}
