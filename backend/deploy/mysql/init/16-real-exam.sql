-- 真题域：一张试卷 + 题目条目
USE study_course;

CREATE TABLE IF NOT EXISTS real_exam (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    course_id   BIGINT       NOT NULL COMMENT '课程',
    title       VARCHAR(120) NOT NULL COMMENT '试卷名称',
    exam_year   INT          NULL COMMENT '年份',
    school      VARCHAR(100) NULL COMMENT '学校/院系',
    material_id BIGINT       NULL COMMENT '来源资料',
    created_by  BIGINT       NOT NULL COMMENT '录入人',
    created_at  DATETIME     NOT NULL COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_course (course_id, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='真题试卷';

CREATE TABLE IF NOT EXISTS real_exam_item (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    exam_id      BIGINT       NOT NULL COMMENT '试卷',
    seq          INT          NOT NULL DEFAULT 1 COMMENT '题号',
    item_type    VARCHAR(20)  NOT NULL DEFAULT 'SINGLE' COMMENT '题型',
    stem         TEXT         NOT NULL COMMENT '题干',
    answer       TEXT         NULL COMMENT '参考答案',
    score        DECIMAL(5, 1) NULL COMMENT '分值',
    page_no      INT          NULL COMMENT '来源页码',
    knowledge_id BIGINT       NULL COMMENT '对应知识点',
    chapter_id   BIGINT       NULL COMMENT '对应章节',
    created_at   DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_exam (exam_id),
    KEY idx_knowledge (knowledge_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='真题题目';
