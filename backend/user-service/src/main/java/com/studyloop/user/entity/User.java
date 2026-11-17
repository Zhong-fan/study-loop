package com.studyloop.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表。
 * <p>
 * password 存的是 BCrypt 加密后的串，不是明文，
 * 所以数据库里永远看不到用户真实密码。
 */
@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    /** STUDENT 学习者 / ADMIN 管理员（部署与账号维护） */
    private String role;

    private LocalDateTime createdAt;

    /** 0 正常，1 已禁用 */
    private Integer deleted;
}
