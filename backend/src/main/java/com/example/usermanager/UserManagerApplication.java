package com.example.usermanager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.usermanager.mapper")
public class UserManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserManagerApplication.class, args);
    }
}
