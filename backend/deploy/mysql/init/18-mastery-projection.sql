-- 作答与掌握度解耦：attempt 是事实，mastery 是可重放的投影
USE study_review;

ALTER TABLE attempt
    ADD COLUMN synced TINYINT NOT NULL DEFAULT 0 COMMENT '掌握度是否已同步';

ALTER TABLE attempt
    ADD COLUMN sync_attempts INT NOT NULL DEFAULT 0 COMMENT '同步重试次数';

ALTER TABLE attempt
    ADD COLUMN last_sync_error VARCHAR(200) NULL COMMENT '最后一次同步失败原因';

ALTER TABLE attempt
    ADD KEY idx_synced (synced, answered_at);

-- 历史数据视为已同步
UPDATE attempt SET synced = 1;

USE study_insight;

CREATE TABLE IF NOT EXISTS attempt_event (
    attempt_id BIGINT   NOT NULL COMMENT '作答 id，天然幂等键',
    applied_at DATETIME NOT NULL COMMENT '处理时间',
    PRIMARY KEY (attempt_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='已处理的作答事件，防止重复投影';
