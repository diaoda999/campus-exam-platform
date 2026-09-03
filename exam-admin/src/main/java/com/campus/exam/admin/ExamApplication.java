package com.campus.exam.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.campus.exam.mapper")
@SpringBootApplication(scanBasePackages = "com.campus.exam")
public class ExamApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamApplication.class, args);
    }
}
