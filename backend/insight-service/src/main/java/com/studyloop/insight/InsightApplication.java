package com.studyloop.insight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 覆盖与调度服务。
 * <p>
 * 负责两件事：算知识点的覆盖率与正确率；按 1/3/7/14 天给每个知识点排下次复习。
 * 它不存题目，也不判分，只消费练习服务产生的作答结果。
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@ComponentScan("com.studyloop")
public class InsightApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsightApplication.class, args);
    }
}
