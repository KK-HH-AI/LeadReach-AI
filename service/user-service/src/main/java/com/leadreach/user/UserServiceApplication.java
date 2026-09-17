package com.leadreach.user;

import org.springframework.boot.SpringApplication; //SPRING BOOT 应用的启动类
import org.springframework.boot.autoconfigure.SpringBootApplication; // 一个组合注解（Annotation）
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;//启用 服务发现客户端 功能

@SpringBootApplication
@EnableDiscoveryClient // 自动向nacos注册服务
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
