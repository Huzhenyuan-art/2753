package com.example.usermanager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.usermanager.entity.Permission;

import java.util.List;

public interface PermissionService extends IService<Permission> {

    List<Permission> getPermissionsByUserId(Long userId);

    List<Permission> getPermissionsByRoleId(Long roleId);

    List<Permission> listAll();
}
