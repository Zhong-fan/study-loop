-- 站内信：目前只有"今日待练"一种
-- 用 dedup_key 保证同一天只提醒一次

USE study_insight;

CREATE TABLE IF NOT EXISTS notification (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT       NOT NULL COMMENT '接收人',
    type       VARCHAR(20)  NOT NULL DEFAULT 'DUE' COMMENT 'DUE 今日待练',
    title      VARCHAR(100) NOT NULL COMMENT '标题',
    content    VARCHAR(255) NOT NULL COMMENT '内容',
    read_at    DATETIME     DEFAULT NULL COMMENT '已读时间，空表示未读',
    dedup_key  VARCHAR(64)  NOT NULL COMMENT '去重键：同一天同类只留一条',
    created_at DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_dedup (user_id, dedup_key),
    KEY idx_user_time (user_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='站内信';
