-- 教育 AI 服务：调用记录
-- 每次调用大模型写一行，用来做限流、成本核算和故障排查

CREATE DATABASE IF NOT EXISTS study_ai
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE study_ai;

CREATE TABLE IF NOT EXISTS ai_log (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT      NOT NULL COMMENT '发起人',
    scene        VARCHAR(30) NOT NULL COMMENT 'GENERATE_QUESTION / GRADE_SUBJECTIVE / KEY_POINTS',
    knowledge_id BIGINT      DEFAULT NULL COMMENT '关联知识点',
    model        VARCHAR(50) NOT NULL COMMENT '模型名',
    cost_ms      BIGINT      NOT NULL DEFAULT 0 COMMENT '耗时毫秒',
    success      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 成功 0 失败',
    error_msg    VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    created_at   DATETIME    NOT NULL COMMENT '调用时间',
    PRIMARY KEY (id),
    KEY idx_user_time (user_id, created_at),
    KEY idx_scene (scene)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI 调用记录';
