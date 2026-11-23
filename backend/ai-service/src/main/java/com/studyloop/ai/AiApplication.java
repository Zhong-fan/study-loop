package com.studyloop.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 教育 AI 服务。
 * <p>
 * 出题、评分要点、主观题判分都走这里，统一管模型、限流和调用记录。
 * 调用失败时不阻塞主流程：内容服务降级为原题，练习服务降级为自评。
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@ComponentScan("com.studyloop")
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
