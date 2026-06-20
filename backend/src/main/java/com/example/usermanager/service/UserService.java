package com.example.usermanager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.usermanager.dto.ChangePasswordDTO;
import com.example.usermanager.dto.LoginUserDTO;
import com.example.usermanager.dto.RefreshTokenDTO;
import com.example.usermanager.entity.User;

import java.util.List;

public interface UserService extends IService<User> {
    LoginUserDTO login(String username, String password);

    RefreshTokenDTO refreshToken(String refreshToken);

    void changePassword(String username, ChangePasswordDTO dto);

    void assignRoles(Long userId, List<Long> roleIds);

    List<Long> getUserRoleIds(Long userId);

    com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> pageWithDept(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page,
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> wrapper,
            Long deptId);
}
