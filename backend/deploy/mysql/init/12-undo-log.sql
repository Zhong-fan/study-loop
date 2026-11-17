-- Seata AT 模式需要的回滚日志表
-- 每个参与全局事务的库都要有一张，结构固定，不要改字段名

CREATE DATABASE IF NOT EXISTS study_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS study_course DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS study_review DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS study_insight DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS study_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE study_auth;
CREATE TABLE IF NOT EXISTS undo_log (
    branch_id     BIGINT       NOT NULL COMMENT '分支事务 id',
    xid           VARCHAR(128) NOT NULL COMMENT '全局事务 id',
    context       VARCHAR(128) NOT NULL COMMENT '回滚信息',
    rollback_info LONGBLOB     NOT NULL COMMENT '回滚数据',
    log_status    INT          NOT NULL COMMENT '0 正常 1 已防御',
    log_created   DATETIME(6)  NOT NULL,
    log_modified  DATETIME(6)  NOT NULL,
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='Seata 回滚日志';

USE study_course;
CREATE TABLE IF NOT EXISTS undo_log LIKE study_auth.undo_log;

USE study_review;
CREATE TABLE IF NOT EXISTS undo_log LIKE study_auth.undo_log;

USE study_insight;
CREATE TABLE IF NOT EXISTS undo_log LIKE study_auth.undo_log;

USE study_ai;
CREATE TABLE IF NOT EXISTS undo_log LIKE study_auth.undo_log;
