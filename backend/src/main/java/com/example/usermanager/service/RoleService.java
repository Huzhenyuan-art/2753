package com.example.usermanager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.usermanager.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {

    List<Role> getRolesByUserId(Long userId);

    List<Role> listAll();
}
