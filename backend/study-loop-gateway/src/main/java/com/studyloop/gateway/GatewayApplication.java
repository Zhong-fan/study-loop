package com.studyloop.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关启动类。
 * <p>
 * 前端只访问 8080，由这里转发到 8081/8082/8083，
 * 顺带把 token 校验掉，业务服务就不用各自再写一遍。
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
