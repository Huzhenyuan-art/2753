package com.example.usermanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.usermanager.annotation.AuditLog;
import com.example.usermanager.common.Result;
import com.example.usermanager.entity.Role;
import com.example.usermanager.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping("/list")
    public Result<List<Role>> list() {
        return Result.success(roleService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Role> getById(@PathVariable Long id) {
        Role role = roleService.getById(id);
        if (role == null) {
            return Result.error(404, "角色不存在");
        }
        return Result.success(role);
    }

    @PostMapping
    @AuditLog(operation = "CREATE", module = "角色管理", description = "新增角色")
    public Result<String> add(@Valid @RequestBody Role role) {
        if (roleService.getOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, role.getCode())) != null) {
            return Result.error(400, "角色编码已存在");
        }
        roleService.save(role);
        return Result.success();
    }

    @PutMapping
    @AuditLog(operation = "UPDATE", module = "角色管理", description = "编辑角色")
    public Result<String> update(@Valid @RequestBody Role role) {
        Role existing = roleService.getOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, role.getCode()));
        if (existing != null && !existing.getId().equals(role.getId())) {
            return Result.error(400, "角色编码已被占用");
        }
        roleService.updateById(role);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @AuditLog(operation = "DELETE", module = "角色管理", description = "删除角色")
    public Result<String> delete(@PathVariable Long id) {
        roleService.removeById(id);
        return Result.success();
    }
}
