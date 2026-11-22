package com.studyloop.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时检查 AI 配置，避免"服务正常但 AI 静默不可用"。
 * <p>
 * 默认只告警；设置 `AI_STRICT=true` 后，配置不可用会直接启动失败，
 * 适合演示/答辩前确认环境。
 */
@Component
public class AiStartupCheck implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AiStartupCheck.class);

    private final AiProperties properties;

    public AiStartupCheck(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            warnOrFail("AI_ENABLED=false，AI 相关接口会返回「未配置」");
            return;
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            warnOrFail("AI_API_KEY 未设置，AI 相关接口会返回「未配置」");
            return;
        }
        LOG.info("AI 已启用: baseUrl={}, model={}, key=(已设置)",
                properties.getBaseUrl(), properties.getModel());
    }

    private void warnOrFail(String message) {
        if (properties.isStrict()) {
            throw new IllegalStateException(message + "（AI_STRICT=true，启动中止）");
        }
        LOG.warn("{}；如需启动即失败，设置 AI_STRICT=true", message);
    }
}
