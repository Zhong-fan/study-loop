package com.studyloop.user.service;

import com.studyloop.user.dto.LoginRequest;
import com.studyloop.user.dto.RegisterRequest;
import com.studyloop.user.vo.LoginVO;

/**
 * 账号业务接口。
 */
public interface AuthService {

    Long register(RegisterRequest request);

    LoginVO login(LoginRequest request);
}
