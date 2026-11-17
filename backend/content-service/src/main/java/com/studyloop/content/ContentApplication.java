package com.studyloop.content;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 内容服务启动类。
 * <p>
 * ComponentScan 指向 com.studyloop，是为了把 common 模块里的
 * 全局异常处理、用户上下文拦截器也扫进来（它们不在本模块的包下面）。
 * Feign 只用来在删除知识点后通知覆盖服务清理掌握记录。
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@ComponentScan("com.studyloop")
public class ContentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentApplication.class, args);
    }
}
