-- 内容服务新增：知识点（节）与解析任务
-- 知识点以「节」为最小单位，挂在章下面

USE study_course;

CREATE TABLE IF NOT EXISTS knowledge (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '归属用户',
    course_id   BIGINT       NOT NULL COMMENT '所属课程',
    chapter_id  BIGINT       NOT NULL COMMENT '所属章',
    name        VARCHAR(80)  NOT NULL COMMENT '节名',
    type        VARCHAR(20)  NOT NULL DEFAULT 'CONCEPT' COMMENT 'CONCEPT 概念 / SKILL 方法 / CASE 案例',
    importance  TINYINT      NOT NULL DEFAULT 3 COMMENT '重要度 1-5',
    page_no     INT          DEFAULT NULL COMMENT '对应资料页码',
    material_id BIGINT       DEFAULT NULL COMMENT '来源资料',
    created_at  DATETIME     NOT NULL COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 1 已删除',
    PRIMARY KEY (id),
    KEY idx_chapter (chapter_id, deleted),
    KEY idx_user_course (user_id, course_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='知识点（节）';

CREATE TABLE IF NOT EXISTS task (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT      NOT NULL COMMENT '发起人',
    material_id  BIGINT      NOT NULL COMMENT '资料',
    type         VARCHAR(20) NOT NULL DEFAULT 'PARSE' COMMENT 'PARSE 解析 / OCR 识别',
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    retry_count  INT         NOT NULL DEFAULT 0 COMMENT '已重试次数',
    error_msg    VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    created_at   DATETIME    NOT NULL COMMENT '创建时间',
    updated_at   DATETIME    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_material (material_id),
    KEY idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='解析任务';
