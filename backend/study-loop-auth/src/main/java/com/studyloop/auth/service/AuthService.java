package com.studyloop.auth.service;

import com.studyloop.auth.dto.LoginRequest;
import com.studyloop.auth.dto.RegisterRequest;
import com.studyloop.auth.vo.LoginVO;

/**
 * 账号业务接口。
 */
public interface AuthService {

    Long register(RegisterRequest request);

    LoginVO login(LoginRequest request);
}
