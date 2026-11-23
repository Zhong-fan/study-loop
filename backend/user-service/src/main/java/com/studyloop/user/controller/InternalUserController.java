package com.studyloop.user.controller;

import com.studyloop.common.result.Result;
import com.studyloop.user.entity.User;
import com.studyloop.user.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部接口：给其它服务判断某个用户是不是管理员。
 * <p>
 * 路径 /api/internal 不在网关路由里，只能服务间直连（仍需 X-Internal-Token）。
 */
@RestController
@RequestMapping("/api/internal/users")
public class InternalUserController {

    private final UserMapper userMapper;

    public InternalUserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/{id}/admin")
    public Result<Boolean> isAdmin(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        boolean admin = user != null
                && "ADMIN".equals(user.getRole())
                && !Integer.valueOf(1).equals(user.getDeleted());
        return Result.ok(admin);
    }
}
