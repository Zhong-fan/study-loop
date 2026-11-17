-- 账号服务数据库

CREATE DATABASE IF NOT EXISTS study_auth
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE study_auth;

CREATE TABLE IF NOT EXISTS sys_user (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username   VARCHAR(20)  NOT NULL COMMENT '用户名',
    password   VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    nickname   VARCHAR(20)  NOT NULL COMMENT '昵称',
    role       VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' COMMENT 'STUDENT 学习者 / ADMIN 管理员',
    created_at DATETIME     NOT NULL COMMENT '注册时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 1 已禁用',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户';
