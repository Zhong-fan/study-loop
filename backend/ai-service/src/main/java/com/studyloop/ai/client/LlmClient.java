package com.studyloop.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.studyloop.ai.config.AiProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 大模型客户端：OpenAI 的 Responses API（POST /responses）。
 * <p>
 * 和 chat/completions 的区别：系统提示放 instructions，用户输入放 input，
 * 取结果要从 output 数组里挑 type=message 的那一项，再取 output_text。
 * 只负责发一次请求拿文本，重试和降级由上层服务决定。
 */
@Component
public class LlmClient {

    private final AiProperties properties;
    private final RestClient restClient;

    public LlmClient(AiProperties properties) {
        this.properties = properties;

        // 不设超时的话，模型卡住会一直占着 Tomcat 线程
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "instructions", systemPrompt,
                "input", userPrompt
        );

        JsonNode response = restClient.post()
                .uri("/responses")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("大模型返回空响应");
        }
        return extractText(response);
    }

    /**
     * 带图片的一次调用：把文本和图片一起放进 user 消息。
     * 图片用 data URL（data:image/png;base64,...），模型侧不需要公网地址。
     */
    public String chatWithImage(String systemPrompt, String userPrompt, String imageDataUrl) {
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "instructions", systemPrompt,
                "input", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "input_text", "text", userPrompt),
                                Map.of("type", "input_image", "image_url", imageDataUrl)
                        )
                ))
        );

        JsonNode response = restClient.post()
                .uri("/responses")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("大模型返回空响应");
        }
        return extractText(response);
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
