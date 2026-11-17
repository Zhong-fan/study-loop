package com.studyloop.practice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 错题与复习服务启动类。
 * <p>
 * 目前是空壳，只验证了"能启动、能注册、能通过网关访问"。
 * 题目与复习卡的业务从 controller 开始往下补。
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan("com.studyloop")
public class PracticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PracticeApplication.class, args);
    }
}
