-- 章节与资料
-- 都放在 study_course 库里：它们和课程是同一个业务边界

USE study_course;

CREATE TABLE IF NOT EXISTS chapter (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    course_id  BIGINT      NOT NULL COMMENT '所属课程',
    user_id    BIGINT      NOT NULL COMMENT '归属用户',
    title      VARCHAR(50) NOT NULL COMMENT '章节名',
    sort       INT         NOT NULL DEFAULT 0 COMMENT '显示顺序',
    created_at DATETIME    NOT NULL COMMENT '创建时间',
    deleted    TINYINT     NOT NULL DEFAULT 0 COMMENT '0 正常 1 已删除',
    PRIMARY KEY (id),
    KEY idx_course_deleted (course_id, deleted)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='章节';

CREATE TABLE IF NOT EXISTS material (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT       NOT NULL COMMENT '归属用户',
    course_id  BIGINT       NOT NULL COMMENT '所属课程',
    chapter_id BIGINT       DEFAULT NULL COMMENT '所属章节，可为空',
    title      VARCHAR(100) NOT NULL COMMENT '显示名称',
    file_name  VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_key   VARCHAR(255) NOT NULL COMMENT '存储层路径，不暴露给前端',
    file_size  BIGINT       NOT NULL DEFAULT 0 COMMENT '字节数',
    file_type  VARCHAR(20)  NOT NULL COMMENT '后缀，如 pdf',
    created_at DATETIME     NOT NULL COMMENT '上传时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 1 已删除',
    PRIMARY KEY (id),
    KEY idx_course_deleted (course_id, deleted),
    KEY idx_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='课程资料';
