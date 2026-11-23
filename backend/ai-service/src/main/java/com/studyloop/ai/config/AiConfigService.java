package com.studyloop.ai.config;

import com.studyloop.ai.entity.AiConfig;
import com.studyloop.ai.mapper.AiConfigMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AI 配置的单一入口。
 * <p>
 * 数据库里有一行配置时以它为准，否则回退到环境变量。
 * 载入后会把有效值写回 {@link AiProperties}，这样所有调用方继续读同一个 bean 即可。
 */
@Service
public class AiConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(AiConfigService.class);
    private static final long CACHE_MILLIS = 10_000;

    private final AiConfigMapper mapper;
    private final AiProperties properties;
    private final ConfigCipher cipher;

    private final boolean envEnabled;
    private final String envBaseUrl;
    private final String envModel;
    private final String envApiKey;

    private volatile AiRuntimeConfig cached;
    private volatile long cachedAt;

    public AiConfigService(AiConfigMapper mapper, AiProperties properties, ConfigCipher cipher) {
        this.mapper = mapper;
        this.properties = properties;
        this.cipher = cipher;
        this.envEnabled = properties.isEnabled();
        this.envBaseUrl = properties.getBaseUrl();
        this.envModel = properties.getModel();
        this.envApiKey = properties.getApiKey();
    }

    @PostConstruct
    public void init() {
        try {
            reload();
        } catch (Exception e) {
            LOG.warn("读取数据库 AI 配置失败，先使用环境变量: {}", e.getMessage());
        }
    }

    /** 调用方每次取配置时最多缓存 10 秒，多实例也能在 10 秒内看到变更 */
    public AiRuntimeConfig current() {
        if (cached == null || System.currentTimeMillis() - cachedAt > CACHE_MILLIS) {
            try {
                reload();
            } catch (Exception e) {
                LOG.warn("刷新 AI 配置失败: {}", e.getMessage());
            }
        }
        return cached == null ? envConfig() : cached;
    }

    public synchronized void reload() {
        AiConfig row = mapper.selectById(1);
        AiRuntimeConfig effective = row == null ? envConfig() : toRuntime(row);
        apply(effective);
        cached = effective;
        cachedAt = System.currentTimeMillis();
    }

    public synchronized AiRuntimeConfig save(AiConfigRequest request, Long userId) {
        AiConfig row = mapper.selectById(1);
        String encrypted = null;
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            encrypted = cipher.encrypt(request.getApiKey().trim());
        }

        if (row == null) {
            row = new AiConfig();
            row.setId(1);
            row.setEnabled(Boolean.FALSE.equals(request.getEnabled()) ? 0 : 1);
            row.setBaseUrl(firstNonBlank(request.getBaseUrl(), envBaseUrl));
            row.setModel(firstNonBlank(request.getModel(), envModel));
            row.setApiKeyEnc(encrypted);
            row.setUpdatedBy(userId);
            row.setUpdatedAt(LocalDateTime.now());
            mapper.insert(row);
        } else {
            if (request.getEnabled() != null) {
                row.setEnabled(request.getEnabled() ? 1 : 0);
            }
            if (request.getBaseUrl() != null && !request.getBaseUrl().isBlank()) {
                row.setBaseUrl(request.getBaseUrl().trim());
            }
            if (request.getModel() != null && !request.getModel().isBlank()) {
                row.setModel(request.getModel().trim());
            }
            if (encrypted != null) {
                row.setApiKeyEnc(encrypted);
            }
            row.setUpdatedBy(userId);
            row.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(row);
        }
        reload();
        return cached;
    }

    /** 测试连接时用：不落库，只构造一份临时配置 */
    public AiRuntimeConfig preview(AiConfigRequest request) {
        AiRuntimeConfig base = current();
        String apiKey = request != null && request.getApiKey() != null && !request.getApiKey().isBlank()
                ? request.getApiKey().trim()
                : base.apiKey();
        return new AiRuntimeConfig(
                request == null || request.getEnabled() == null ? base.enabled() : request.getEnabled(),
                firstNonBlank(request == null ? null : request.getBaseUrl(), base.baseUrl()),
                firstNonBlank(request == null ? null : request.getModel(), base.model()),
                apiKey,
                base.fromDb(),
                base.updatedAt(),
                base.updatedBy());
    }

    private AiRuntimeConfig toRuntime(AiConfig row) {
        boolean enabled = row.getEnabled() == null || row.getEnabled() == 1;
        String baseUrl = firstNonBlank(row.getBaseUrl(), envBaseUrl);
        String model = firstNonBlank(row.getModel(), envModel);
        String apiKey = envApiKey;
        if (row.getApiKeyEnc() != null && !row.getApiKeyEnc().isBlank()) {
            apiKey = cipher.decrypt(row.getApiKeyEnc());
        }
        return new AiRuntimeConfig(enabled, baseUrl, model, apiKey, true, row.getUpdatedAt(), row.getUpdatedBy());
    }

    private AiRuntimeConfig envConfig() {
        return new AiRuntimeConfig(envEnabled, envBaseUrl, envModel, envApiKey, false, null, null);
    }

    private void apply(AiRuntimeConfig config) {
        properties.setEnabled(config.enabled());
        properties.setBaseUrl(config.baseUrl());
        properties.setModel(config.model());
        properties.setApiKey(config.apiKey());
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
