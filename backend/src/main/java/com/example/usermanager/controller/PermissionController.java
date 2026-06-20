package com.example.usermanager.controller;

import com.example.usermanager.annotation.AuditLog;
import com.example.usermanager.common.Result;
import com.example.usermanager.entity.Permission;
import com.example.usermanager.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permission")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @GetMapping("/list")
    public Result<List<Permission>> list() {
        return Result.success(permissionService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Permission> getById(@PathVariable Long id) {
        Permission permission = permissionService.getById(id);
        if (permission == null) {
            return Result.error(404, "权限不存在");
        }
        return Result.success(permission);
    }

    @PostMapping
    @AuditLog(operation = "CREATE", module = "权限管理", description = "新增权限")
    public Result<String> add(@Valid @RequestBody Permission permission) {
        permissionService.save(permission);
        return Result.success();
    }

    @PutMapping
    @AuditLog(operation = "UPDATE", module = "权限管理", description = "编辑权限")
    public Result<String> update(@Valid @RequestBody Permission permission) {
        permissionService.updateById(permission);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @AuditLog(operation = "DELETE", module = "权限管理", description = "删除权限")
    public Result<String> delete(@PathVariable Long id) {
        permissionService.removeById(id);
        return Result.success();
    }
}
