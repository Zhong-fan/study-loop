package com.studyloop.common.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 内部调用校验：只有网关或兄弟服务带着约定的 token 才能访问。
 * <p>
 * 网关在所有转发请求上统一加 `X-Internal-Token`，服务之间用 Feign 拦截器加。
 * 这样即使有人直接访问 10082 这样的服务端口，没有 token 也会被拒。
 */
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";

    private final String expected;

    public InternalTokenFilter(String expected) {
        this.expected = expected;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 文档与健康检查放行，方便本地排查
        return path.startsWith("/doc.html")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String actual = request.getHeader(HEADER);
        if (expected == null || expected.isBlank() || expected.equals(actual)) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"缺少内部调用凭证\",\"data\":null}");
    }
}
