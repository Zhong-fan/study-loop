package com.studyloop.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.studyloop.ai.config.AiProperties;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 大模型客户端：OpenAI Responses API（POST /responses）。
 * <p>
 * 每次调用都从 {@link AiProperties} 读当前生效的 baseUrl / model / key，
 * 这样管理员在后台改完配置后不需要重启服务。
 */
@Component
public class LlmClient {

    private final AiProperties properties;
    private final Map<String, RestClient> clients = new ConcurrentHashMap<>();

    public LlmClient(AiProperties properties) {
        this.properties = properties;
    }

    public String chat(String systemPrompt, String userPrompt) {
        return call(systemPrompt, Map.of(
                "model", properties.getModel(),
                "instructions", systemPrompt,
                "input", userPrompt
        ));
    }

    /**
     * 带图片的一次调用：把文本和图片一起放进 user 消息。
     * 图片用 data URL（data:image/png;base64,...），模型侧不需要公网地址。
     */
    public String chatWithImage(String systemPrompt, String userPrompt, String imageDataUrl) {
        return call(systemPrompt, Map.of(
                "model", properties.getModel(),
                "instructions", systemPrompt,
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "input_text", "text", userPrompt),
                                Map.of("type", "input_image", "image_url", imageDataUrl)
                        )
                ))
        ));
    }

    /** 测试连接：发一个极小请求，返回耗时毫秒 */
    public long ping(String baseUrl, String model, String apiKey) {
        long start = System.currentTimeMillis();
        callWith(baseUrl, model, apiKey, Map.of(
                "model", model,
                "instructions", "只回复 OK",
                "input", "ping"
        ));
        return System.currentTimeMillis() - start;
    }

    private String call(String systemPrompt, Map<String, Object> body) {
        return callWith(properties.getBaseUrl(), properties.getModel(), properties.getApiKey(), body);
    }

    private String callWith(String baseUrl, String model, String apiKey, Map<String, Object> body) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "AI 未配置：缺少 API Key");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "AI 未配置：缺少 Base URL");
        }

        JsonNode response = clientFor(baseUrl).post()
                .uri("/responses")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("大模型返回空响应");
        }
        return extractText(response);
    }

    private RestClient clientFor(String baseUrl) {
        return clients.computeIfAbsent(baseUrl, url -> {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
            factory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
            return RestClient.builder()
                    .baseUrl(url)
                    .requestFactory(factory)
                    .build();
        });
    }

    /**
     * 从 Responses API 的结构里取正文：
     * output[] → 找到 type=message → content[] → 找 type=output_text → text。
     * 有些实现会把正文直接放在 output_text 字段，这里一并兜住。
     */
    private String extractText(JsonNode response) {
        JsonNode output = response.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                if ("message".equals(item.path("type").asText())) {
                    for (JsonNode content : item.path("content")) {
                        if ("output_text".equals(content.path("type").asText())) {
                            String text = content.path("text").asText("");
                            if (!text.isBlank()) {
                                return text;
                            }
                        }
                    }
                }
            }
        }

        String fallback = response.path("output_text").asText("");
        if (!fallback.isBlank()) {
            return fallback;
        }
        throw new IllegalStateException("大模型响应里没有正文");
    }
}
