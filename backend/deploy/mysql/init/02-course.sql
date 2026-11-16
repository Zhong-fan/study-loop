-- 课程服务数据库
-- 一个服务一个库，course 服务不允许访问 study_auth / study_review

CREATE DATABASE IF NOT EXISTS study_course
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE study_course;

CREATE TABLE IF NOT EXISTS course (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '归属用户',
    name        VARCHAR(50)  NOT NULL COMMENT '课程名称',
    description VARCHAR(200) DEFAULT NULL COMMENT '简介',
    color       VARCHAR(20)  DEFAULT '#409EFF' COMMENT '颜色标记',
    created_at  DATETIME     NOT NULL COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 1 已删除',
    PRIMARY KEY (id),
    KEY idx_user_deleted (user_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='课程';
