package com.medical;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 智能医疗服务管理系统 - 后端启动类
 */
@SpringBootApplication
@MapperScan("com.medical.mapper")
public class MedicalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalServiceApplication.class, args);
    }
}
