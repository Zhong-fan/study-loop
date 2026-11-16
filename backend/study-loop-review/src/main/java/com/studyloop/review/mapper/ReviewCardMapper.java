package com.studyloop.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyloop.review.entity.ReviewCard;
import org.apache.ibatis.annotations.Mapper;

/**
 * 复习卡表的数据访问。
 */
@Mapper
public interface ReviewCardMapper extends BaseMapper<ReviewCard> {
}
