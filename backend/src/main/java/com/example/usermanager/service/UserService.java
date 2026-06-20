package com.example.usermanager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.usermanager.dto.ChangePasswordDTO;
import com.example.usermanager.dto.LoginUserDTO;
import com.example.usermanager.entity.User;

import java.util.List;

public interface UserService extends IService<User> {
    LoginUserDTO login(String username, String password);

    void changePassword(String username, ChangePasswordDTO dto);

    void assignRoles(Long userId, List<Long> roleIds);

    List<Long> getUserRoleIds(Long userId);
}
