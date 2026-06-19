package com.example.usermanager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class UploadDirConfig {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void initUploadDir() {
        try {
            Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                log.info("上传目录已创建: {}", dirPath);
            } else {
                log.info("上传目录已存在: {}", dirPath);
            }
        } catch (Exception e) {
            log.error("创建上传目录失败: uploadDir={}, error={}", uploadDir, e.getMessage(), e);
        }
    }
}
