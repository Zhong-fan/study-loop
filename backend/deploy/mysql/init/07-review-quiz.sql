-- 组卷自测

USE study_review;

CREATE TABLE IF NOT EXISTS quiz (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT      NOT NULL COMMENT '归属用户',
    course_id      BIGINT      NOT NULL COMMENT '所属课程',
    title          VARCHAR(80) NOT NULL COMMENT '试卷标题',
    question_count INT         NOT NULL DEFAULT 0 COMMENT '题目数量',
    status         TINYINT     NOT NULL DEFAULT 0 COMMENT '0 进行中 1 已交卷',
    score          DOUBLE      DEFAULT NULL COMMENT '百分制得分',
    created_at     DATETIME    NOT NULL COMMENT '创建时间',
    submitted_at   DATETIME    DEFAULT NULL COMMENT '交卷时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='自测试卷';

CREATE TABLE IF NOT EXISTS quiz_item (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    quiz_id     BIGINT NOT NULL COMMENT '试卷',
    question_id BIGINT NOT NULL COMMENT '题目',
    sort        INT    NOT NULL DEFAULT 0 COMMENT '题号',
    PRIMARY KEY (id),
    KEY idx_quiz (quiz_id, sort)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='试卷题目';

CREATE TABLE IF NOT EXISTS quiz_answer (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    quiz_id     BIGINT   NOT NULL COMMENT '试卷',
    question_id BIGINT   NOT NULL COMMENT '题目',
    user_answer TEXT     COMMENT '作答 JSON',
    score       DOUBLE   NOT NULL DEFAULT 0 COMMENT '本题得分',
    correct     TINYINT  NOT NULL DEFAULT 0 COMMENT '1 全对 0 否则',
    created_at  DATETIME NOT NULL COMMENT '作答时间',
    PRIMARY KEY (id),
    KEY idx_quiz (quiz_id),
    KEY idx_user_question (question_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='试卷作答';
