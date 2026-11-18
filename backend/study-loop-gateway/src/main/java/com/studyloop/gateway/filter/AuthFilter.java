package com.studyloop.gateway.filter;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 登录校验。
 * <p>
 * 流程：白名单直接放行 → 取 Authorization 头 → 验签 → 把 userId 塞进 X-User-Id → 转发。
 * 业务服务只认这个头，不再自己解析 token。
 * <p>
 * 这里的 SECRET 必须和 auth 服务里签名用的保持一致。
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    /** 必须和 user-service 的 JwtHelper 用同一个 JWT_SECRET，否则验签失败 */
    private static final byte[] SECRET = System.getenv()
            .getOrDefault("JWT_SECRET", "study-loop-secret-2026-change-me")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);

    /** 登录和注册不需要 token */
    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (WHITE_LIST.contains(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return reject(exchange);
        }

        Long userId = parseUserId(authorization.substring("Bearer ".length()));
        if (userId == null) {
            return reject(exchange);
        }

        ServerHttpRequest newRequest = exchange.getRequest()
                .mutate()
                .header("X-User-Id", String.valueOf(userId))
                .build();

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    /** 验签并取出用户 id，任何一步失败都返回 null */
    private Long parseUserId(String token) {
        try {
            if (!JWTUtil.verify(token, SECRET)) {
                return null;
            }
            JWT jwt = JWTUtil.parseToken(token);
            Object userId = jwt.getPayload("userId");
            Object expireAt = jwt.getPayload("exp");
            if (userId == null || expireAt == null) {
                return null;
            }
            long now = System.currentTimeMillis() / 1000;
            if (Long.parseLong(expireAt.toString()) < now) {
                return null;
            }
            return Long.parseLong(userId.toString());
        } catch (Exception e) {
            // token 格式不对、被改过、过期，都在这里兜住
            return null;
        }
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"code\":2001,\"message\":\"未登录或登录已过期\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 数字越小越先执行，要在路由转发之前
        return -100;
    }
}
