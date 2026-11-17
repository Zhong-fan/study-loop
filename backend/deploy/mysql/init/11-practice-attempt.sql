-- 练习服务：作答记录
-- 掌握度不在这里算，这里只如实记流水，insight 服务消费它

USE study_review;

CREATE TABLE IF NOT EXISTS attempt (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT      NOT NULL COMMENT '作答人',
    question_id  BIGINT      NOT NULL COMMENT '题目',
    knowledge_id BIGINT      DEFAULT NULL COMMENT '题目挂的知识点',
    score        DOUBLE      NOT NULL DEFAULT 0 COMMENT '得分 0~1',
    grader       VARCHAR(10) NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO 自动 / SELF 自评 / AI 大模型',
    cost_ms      BIGINT      NOT NULL DEFAULT 0 COMMENT '作答耗时',
    answered_at  DATETIME    NOT NULL COMMENT '作答时间',
    PRIMARY KEY (id),
    KEY idx_user_time (user_id, answered_at),
    KEY idx_knowledge (knowledge_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='作答记录';
