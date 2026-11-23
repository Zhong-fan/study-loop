package com.studyloop.ai.client;

import com.studyloop.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 只用来判断当前用户是不是管理员。
 * 路径 /api/internal 不在网关路由里，直连也需要内部 token。
 */
@FeignClient(name = "user-service", contextId = "userAdminClient", path = "/api/internal/users")
public interface UserAdminClient {

    @GetMapping("/{id}/admin")
    Result<Boolean> isAdmin(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId);
}
