package com.studyloop.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 账号服务启动类。
 * <p>
 * 只负责注册、登录、签发 token，业务数据一律不放在这个服务里。
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan("com.studyloop")
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
