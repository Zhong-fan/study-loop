package com.studyloop.user.util;

import cn.hutool.jwt.JWT;

/**
 * 生成和解析 token。
 * <p>
 * 用 Hutool 的 JWT，不引 spring-security：这里只需要"签一个串、验一个串"，
 * 引一整套安全框架反而要看很多配置。
 * <p>
 * SECRET 放在这里只是为了入门简单，真实项目要放到配置文件或环境变量里。
 */
public class JwtHelper {

    /**
     * 签名密钥。默认值和网关保持一致，生产环境通过 JWT_SECRET 覆盖，
     * 两个服务必须用同一个值，否则网关验签必然失败。
     */
    private static final String DEFAULT_SECRET = "study-loop-secret-2026-change-me";

    private static final String SECRET_TEXT = System.getenv().getOrDefault(
            "JWT_SECRET", DEFAULT_SECRET);

    static {
        if (System.getenv("JWT_SECRET") == null) {
            org.slf4j.LoggerFactory.getLogger(JwtHelper.class)
                    .warn("JWT_SECRET 未设置，正在使用仓库里的默认密钥；部署前必须覆盖");
        }
    }

    private static final byte[] SECRET = SECRET_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8);

    /** token 有效期：7 天 */
    public static final long EXPIRE_SECONDS = 7 * 24 * 60 * 60L;

    private JwtHelper() {
    }

    /** 把用户 id 写进 token */
    public static String create(Long userId) {
        long expireAt = System.currentTimeMillis() / 1000 + EXPIRE_SECONDS;
        return JWT.create()
                .setPayload("userId", userId)
                .setPayload("exp", expireAt)
                .setKey(SECRET)
                .sign();
    }

    /** 校验签名，签名不对或过期都返回 false */
    public static boolean verify(String token) {
        if (!cn.hutool.jwt.JWTUtil.verify(token, SECRET)) {
            return false;
        }
        Long expireAt = getExpireAt(token);
        return expireAt != null && expireAt > System.currentTimeMillis() / 1000;
    }

    /** 取出 userId，取不到返回 null */
    public static Long getUserId(String token) {
        Object value = cn.hutool.jwt.JWTUtil.parseToken(token).getPayload("userId");
        if (value == null) {
            return null;
        }
        return Long.valueOf(value.toString());
    }

    private static Long getExpireAt(String token) {
        Object value = cn.hutool.jwt.JWTUtil.parseToken(token).getPayload("exp");
        if (value == null) {
            return null;
        }
        return Long.valueOf(value.toString());
    }
}
