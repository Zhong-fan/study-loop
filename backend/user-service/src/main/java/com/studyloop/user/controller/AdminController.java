package com.studyloop.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.studyloop.common.context.UserContext;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.result.Result;
import com.studyloop.user.entity.User;
import com.studyloop.user.mapper.UserMapper;
import com.studyloop.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员接口：账号维护。
 * <p>
 * 只有 role=ADMIN 的用户能调用。角色存在库里，每次请求现查，
 * 避免把角色塞进 token 后"改了角色还要重新登录才生效"。
 */
@Tag(name = "管理员")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserMapper userMapper;

    public AdminController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<List<UserVO>> users() {
        requireAdmin();

        List<UserVO> result = new ArrayList<>();
        for (User user : userMapper.selectList(new QueryWrapper<User>().orderByAsc("id"))) {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setRole(user.getRole());
            vo.setCreatedAt(user.getCreatedAt());
            vo.setDeleted(user.getDeleted());
            result.add(vo);
        }
        return Result.ok(result);
    }

    @Operation(summary = "禁用 / 启用账号")
    @PutMapping("/users/{id}/status")
    public Result<Void> setStatus(@PathVariable Long id, @RequestParam int deleted) {
        requireAdmin();

        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "不能禁用管理员自己");
        }
        user.setDeleted(deleted == 0 ? 0 : 1);
        userMapper.updateById(user);
        return Result.ok();
    }

    private void requireAdmin() {
        Long userId = UserContext.requireUserId();

        User user = userMapper.selectById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
    }
}
