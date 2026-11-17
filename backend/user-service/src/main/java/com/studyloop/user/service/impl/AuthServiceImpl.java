package com.studyloop.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.studyloop.user.dto.LoginRequest;
import com.studyloop.user.dto.RegisterRequest;
import com.studyloop.user.entity.User;
import com.studyloop.user.mapper.UserMapper;
import com.studyloop.user.service.AuthService;
import com.studyloop.user.util.JwtHelper;
import com.studyloop.user.vo.LoginVO;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 注册与登录。
 * <p>
 * 密码用 BCrypt 加密：同样的密码每次加密结果都不同，
 * 所以不能"把用户输入的密码加密后和数据库比对"，只能 checkpw。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final String adminUsername;

    public AuthServiceImpl(UserMapper userMapper,
                           @Value("${app.admin.username:admin}") String adminUsername) {
        this.userMapper = userMapper;
        this.adminUsername = adminUsername;
    }

    @Override
    public Long register(RegisterRequest request) {
        String username = request.getUsername().trim();

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        User exists = userMapper.selectOne(wrapper);
        if (exists != null) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "用户名已被占用");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setNickname(request.getNickname() == null || request.getNickname().isBlank()
                ? username
                : request.getNickname().trim());
        // 只有一个管理员：注册名等于配置里的 admin 就是管理员，其余都是学生
        user.setRole(username.equals(adminUsername) ? "ADMIN" : "STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        user.setDeleted(0);

        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public LoginVO login(LoginRequest request) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", request.getUsername().trim());
        User user = userMapper.selectOne(wrapper);

        // 用户不存在和密码错误返回同一句提示，避免别人拿接口试出哪些用户名存在
        if (user == null || user.getDeleted() == 1
                || !BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "用户名或密码错误");
        }

        LoginVO vo = new LoginVO();
        vo.setToken(JwtHelper.create(user.getId()));
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole() == null ? "STUDENT" : user.getRole());
        return vo;
    }
}
