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

CREATE TABLE IF NOT EXISTS review_card (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT      NOT NULL COMMENT '归属用户',
    question_id    BIGINT      NOT NULL COMMENT '对应题目',
    state          VARCHAR(20) NOT NULL DEFAULT 'NEW' COMMENT 'NEW / REVIEW',
    ease_factor    DOUBLE      NOT NULL DEFAULT 2.5 COMMENT '难度系数',
    interval_days  INT         NOT NULL DEFAULT 0 COMMENT '当前间隔（天）',
    repetitions    INT         NOT NULL DEFAULT 0 COMMENT '连续答对次数',
    lapses         INT         NOT NULL DEFAULT 0 COMMENT '累计答错次数',
    due_date       DATE        NOT NULL COMMENT '到期日',
    last_review_at DATETIME    DEFAULT NULL COMMENT '最近一次复习时间',
    created_at     DATETIME    NOT NULL COMMENT '创建时间',
    deleted        TINYINT     NOT NULL DEFAULT 0 COMMENT '0 正常 1 已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_question (question_id),
    KEY idx_user_due (user_id, due_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='复习卡';

CREATE TABLE IF NOT EXISTS review_log (
    id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    card_id       BIGINT   NOT NULL COMMENT '复习卡',
    user_id       BIGINT   NOT NULL COMMENT '归属用户',
    rating        TINYINT  NOT NULL COMMENT '0 忘记 1 模糊 2 记住',
    prev_interval INT      NOT NULL DEFAULT 0 COMMENT '复习前间隔',
    next_interval INT      NOT NULL DEFAULT 0 COMMENT '复习后间隔',
    prev_ease     DOUBLE   NOT NULL COMMENT '复习前难度系数',
    next_ease     DOUBLE   NOT NULL COMMENT '复习后难度系数',
    cost_ms       BIGINT   NOT NULL DEFAULT 0 COMMENT '答题耗时（毫秒）',
    reviewed_at   DATETIME NOT NULL COMMENT '复习时间',
    PRIMARY KEY (id),
    KEY idx_user_time (user_id, reviewed_at),
    KEY idx_card (card_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='复习流水';
