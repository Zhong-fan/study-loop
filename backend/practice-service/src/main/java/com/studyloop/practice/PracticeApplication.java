package com.studyloop.practice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 练习与判分服务启动类。
 * <p>
 * 负责题目、组卷、作答判分；判完分回调 insight 服务更新掌握度。
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@ComponentScan("com.studyloop")
public class PracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PracticeApplication.class, args);
    }
}
