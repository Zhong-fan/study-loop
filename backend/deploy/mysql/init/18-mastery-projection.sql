-- 作答与掌握度解耦：attempt 是事实，mastery 是可重放的投影
USE study_review;

-- 幂等辅助：列/索引不存在才执行，重复跑不会报错
DROP PROCEDURE IF EXISTS sl_add_column;
DROP PROCEDURE IF EXISTS sl_add_index;
DELIMITER $$
CREATE PROCEDURE sl_add_column(IN p_schema VARCHAR(64), IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_def TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = p_schema AND table_name = p_table AND column_name = p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_schema, '`.`', p_table, '` ADD COLUMN `', p_column, '` ', p_def);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
CREATE PROCEDURE sl_add_index(IN p_schema VARCHAR(64), IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_def TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = p_schema AND table_name = p_table AND index_name = p_index) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_schema, '`.`', p_table, '` ADD ', p_def);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;


CALL sl_add_column('study_review', 'attempt', 'synced', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''掌握度是否已同步''');

CALL sl_add_column('study_review', 'attempt', 'sync_attempts', 'INT NOT NULL DEFAULT 0 COMMENT ''同步重试次数''');

CALL sl_add_column('study_review', 'attempt', 'last_sync_error', 'VARCHAR(200) NULL COMMENT ''最后一次同步失败原因''');

CALL sl_add_index('study_review', 'attempt', 'idx_synced',
    'KEY idx_synced (synced, answered_at)');


USE study_insight;

CREATE TABLE IF NOT EXISTS attempt_event (
    attempt_id BIGINT   NOT NULL COMMENT '作答 id，天然幂等键',
    applied_at DATETIME NOT NULL COMMENT '处理时间',
    PRIMARY KEY (attempt_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='已处理的作答事件，防止重复投影';

DROP PROCEDURE IF EXISTS sl_add_column;
DROP PROCEDURE IF EXISTS sl_add_index;
