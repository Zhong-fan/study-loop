package com.studyloop.user.controller;

import com.studyloop.user.dto.LoginRequest;
import com.studyloop.user.dto.RegisterRequest;
import com.studyloop.user.service.AuthService;
import com.studyloop.user.vo.LoginVO;
import com.studyloop.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号接口。
 * <p>
 * 登录和注册不需要 token，所以它们在网关的白名单里。
 */
@Tag(name = "账号")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<Long> register(@RequestBody @Valid RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginRequest request) {
        return Result.ok(authService.login(request));
    }
}
