-- 题目与复习
-- 一个服务一个库：错题服务只允许访问 study_review

CREATE DATABASE IF NOT EXISTS study_review
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE study_review;

CREATE TABLE IF NOT EXISTS question (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT       NOT NULL COMMENT '归属用户',
    course_id  BIGINT       NOT NULL COMMENT '所属课程（跨服务只存 id）',
    chapter_id BIGINT       DEFAULT NULL COMMENT '所属章节',
    knowledge_id BIGINT     DEFAULT NULL COMMENT '所属知识点',
    material_id BIGINT      DEFAULT NULL COMMENT '来源资料',
    page_no    INT          DEFAULT NULL COMMENT '来源页码',
    quote      TEXT         DEFAULT NULL COMMENT '出题时选中的原文',
    type       VARCHAR(20)  NOT NULL COMMENT 'SINGLE/MULTIPLE/JUDGE/BLANK/SUBJECTIVE/IMAGE',
    stem       TEXT         NOT NULL COMMENT '题干',
    options    TEXT         DEFAULT NULL COMMENT '选项 JSON 数组',
    answer     TEXT         NOT NULL COMMENT '答案 JSON 数组',
    analysis   TEXT         DEFAULT NULL COMMENT '解析',
    difficulty TINYINT      NOT NULL DEFAULT 3 COMMENT '难度 1-5',
    status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 待复习 1 已掌握',
    created_at DATETIME     NOT NULL COMMENT '创建时间',
    updated_at DATETIME     NOT NULL COMMENT '更新时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 1 已删除',
    PRIMARY KEY (id),
    KEY idx_user_course (user_id, course_id, deleted),
    KEY idx_user_type (user_id, type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='题目';
