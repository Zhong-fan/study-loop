-- 覆盖与调度服务：掌握度
-- 每个「用户 + 知识点」一行，记录正确率与下一次复习时间

CREATE DATABASE IF NOT EXISTS study_insight
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE study_insight;

CREATE TABLE IF NOT EXISTS mastery (
    id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT   NOT NULL COMMENT '用户',
    knowledge_id  BIGINT   NOT NULL COMMENT '知识点',
    total_count   INT      NOT NULL DEFAULT 0 COMMENT '练过几次',
    correct_count INT      NOT NULL DEFAULT 0 COMMENT '对几次',
    accuracy      DOUBLE   NOT NULL DEFAULT 0 COMMENT '正确率 0~1',
    stage         TINYINT  NOT NULL DEFAULT 0 COMMENT '阶梯序号 0~3，对应 1/3/7/14 天',
    next_review   DATE     NOT NULL COMMENT '下次复习日期',
    updated_at    DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_knowledge (user_id, knowledge_id),
    KEY idx_user_next (user_id, next_review)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='知识点掌握度';
