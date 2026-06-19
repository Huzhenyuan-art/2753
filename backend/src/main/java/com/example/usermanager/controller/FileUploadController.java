package com.example.usermanager.controller;

import com.example.usermanager.common.Result;
import com.example.usermanager.entity.User;
import com.example.usermanager.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private UserService userService;

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "仅支持上传图片文件（如 JPG、PNG、GIF 等）");
        }

        if (file.getSize() > 20 * 1024 * 1024) {
            return Result.error(413, "图片大小不能超过20MB，请压缩后重试");
        }

        try {
            Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                log.info("创建上传目录: {}", dirPath);
            }

            if (!Files.isWritable(dirPath)) {
                log.error("上传目录不可写: {}", dirPath);
                return Result.error(500, "服务器存储异常，请联系管理员");
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;

            Path filePath = dirPath.resolve(filename);
            file.transferTo(filePath.toFile());

            log.info("头像上传成功: {} -> {}", originalFilename, filePath);

            String avatarUrl = "/uploads/avatars/" + filename;

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.lambdaQuery().eq(User::getUsername, username).one();
            if (user != null) {
                user.setAvatar(avatarUrl);
                userService.updateById(user);
            }

            return Result.success(avatarUrl);
        } catch (IOException e) {
            log.error("头像上传失败, uploadDir={}, error={}", uploadDir, e.getMessage(), e);
            return Result.error(500, "头像上传失败，请稍后重试");
        } catch (Exception e) {
            log.error("头像上传未知异常: {}", e.getMessage(), e);
            return Result.error(500, "系统异常，请稍后重试");
        }
    }
}
