package com.studyloop.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyloop.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表的数据访问。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
