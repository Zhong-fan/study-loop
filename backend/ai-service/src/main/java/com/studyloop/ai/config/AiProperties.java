package com.studyloop.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 大模型配置。
 * <p>
 * key 从环境变量读，仓库里不出现明文：
 * <pre>
 *   AI_API_KEY=sk-xxx
 * </pre>
 * 没配 key 时服务照常启动，接口返回"未配置"，前端降级到原题与自评。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** 是否启用，关掉时所有 AI 接口直接返回未配置 */
    private boolean enabled = false;

    /** OpenAI 兼容的接口地址，DeepSeek、通义、Kimi 都行 */
    private String baseUrl = "https://api.deepseek.com";

    private String model = "deepseek-chat";

    private String apiKey = "";

    private int timeoutSeconds = 60;

    public boolean usable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
