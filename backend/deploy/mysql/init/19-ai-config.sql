-- AI 配置：管理员可改，key 加密存储
USE study_ai;

CREATE TABLE IF NOT EXISTS ai_config (
    id          TINYINT      NOT NULL DEFAULT 1 COMMENT '单行配置',
    enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 AI',
    base_url    VARCHAR(255) NOT NULL COMMENT 'OpenAI 兼容地址',
    model       VARCHAR(100) NOT NULL COMMENT '模型名',
    api_key_enc VARCHAR(512) NULL COMMENT 'AES-GCM 加密后的 key',
    updated_by  BIGINT       NULL COMMENT '最后修改人',
    updated_at  DATETIME     NOT NULL COMMENT '最后修改时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='AI 运行时配置';
