package com.studyloop.ai.config;

import lombok.Data;

@Data
public class AiConfigRequest {

    private Boolean enabled;

    private String baseUrl;

    private String model;

    /** 留空表示保持原 key 不变 */
    private String apiKey;
}
