package com.studyloop.common.web;

import com.studyloop.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 把网关传过来的 X-User-Id 存进 UserContext。
 * <p>
 * 这里不校验 token —— token 已经在网关校验过了，
 * 业务服务只信任这个请求头（所以业务服务的端口不要直接暴露到公网）。
 */
public class UserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            UserContext.set(Long.valueOf(userId));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 一定要清理，Tomcat 线程是复用的
        UserContext.clear();
    }
}
