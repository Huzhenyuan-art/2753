package com.example.usermanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.usermanager.entity.Permission;
import com.example.usermanager.mapper.PermissionMapper;
import com.example.usermanager.service.PermissionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    @Override
    public List<Permission> getPermissionsByUserId(Long userId) {
        return baseMapper.selectPermissionsByUserId(userId);
    }

    @Override
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        return baseMapper.selectPermissionsByRoleId(roleId);
    }

    @Override
    public List<Permission> listAll() {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Permission::getId);
        return this.list(wrapper);
    }
}
