package com.studyloop.course;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 课程服务启动类。
 * <p>
 * ComponentScan 指向 com.studyloop，是为了把 common 模块里的
 * 全局异常处理、用户上下文拦截器也扫进来（它们不在本模块的包下面）。
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan("com.studyloop")
public class CourseApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourseApplication.class, args);
    }
}
