package com.studyloop.ai.config;

import java.time.LocalDateTime;

/**
 * 运行时生效的 AI 配置：可能来自数据库，也可能来自环境变量。
 */
public record AiRuntimeConfig(boolean enabled,
                              String baseUrl,
                              String model,
                              String apiKey,
                              boolean fromDb,
                              LocalDateTime updatedAt,
                              Long updatedBy) {

    public boolean usable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
