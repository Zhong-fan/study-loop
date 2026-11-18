package com.studyloop.user.vo;

import lombok.Data;

/**
 * 登录成功后返回给前端的内容。
 * <p>
 * 注意这里没有 password 字段，避免不小心把密码返回出去。
 */
@Data
public class LoginVO {

    private String token;
    private Long userId;
    private String username;
    private String nickname;

    /** STUDENT / ADMIN，前端据此决定显不显示管理入口 */
    private String role;
}
