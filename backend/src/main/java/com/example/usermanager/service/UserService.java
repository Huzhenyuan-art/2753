package com.example.usermanager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.usermanager.dto.ChangePasswordDTO;
import com.example.usermanager.dto.LoginUserDTO;
import com.example.usermanager.dto.RefreshTokenDTO;
import com.example.usermanager.dto.UserImportResult;
import com.example.usermanager.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.List;

public interface UserService extends IService<User> {
    LoginUserDTO login(String username, String password);

    RefreshTokenDTO refreshToken(String refreshToken);

    void changePassword(String username, ChangePasswordDTO dto);

    void assignRoles(Long userId, List<Long> roleIds);

    List<Long> getUserRoleIds(Long userId);

    Page<User> pageWithDept(Page<User> page, LambdaQueryWrapper<User> wrapper, Long deptId);

    void downloadTemplate(OutputStream outputStream);

    UserImportResult importUsers(MultipartFile file);

    void exportUsers(OutputStream outputStream, LambdaQueryWrapper<User> wrapper, Long deptId);
}
