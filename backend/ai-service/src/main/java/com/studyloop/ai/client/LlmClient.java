package com.studyloop.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.studyloop.ai.config.AiProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 大模型客户端：OpenAI 兼容的 /v1/chat/completions。
 * <p>
 * 只负责发一次请求拿文本，重试和降级由上层服务决定。
 */
@Component
public class LlmClient {

    private final AiProperties properties;
    private final RestClient restClient;

    public LlmClient(AiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        JsonNode response = restClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("大模型返回空响应");
        }
        return response.path("choices").path(0).path("message").path("content").asText("");
    }
}
