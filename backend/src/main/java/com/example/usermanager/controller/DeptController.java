package com.example.usermanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.usermanager.annotation.AuditLog;
import com.example.usermanager.common.Result;
import com.example.usermanager.entity.Dept;
import com.example.usermanager.service.DeptService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dept")
public class DeptController {

    @Autowired
    private DeptService deptService;

    @GetMapping("/tree")
    public Result<List<Dept>> tree() {
        return Result.success(deptService.listTree());
    }

    @GetMapping("/list")
    public Result<List<Dept>> list() {
        return Result.success(deptService.list(new LambdaQueryWrapper<Dept>().orderByAsc(Dept::getSortOrder)));
    }

    @GetMapping("/{id}")
    public Result<Dept> getById(@PathVariable Long id) {
        Dept dept = deptService.getById(id);
        if (dept == null) {
            return Result.error(404, "部门不存在");
        }
        return Result.success(dept);
    }

    @PostMapping
    @AuditLog(operation = "CREATE", module = "部门管理", description = "新增部门")
    public Result<String> add(@Valid @RequestBody Dept dept) {
        if (deptService.getOne(new LambdaQueryWrapper<Dept>().eq(Dept::getCode, dept.getCode())) != null) {
            return Result.error(400, "部门编码已存在");
        }
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }
        deptService.save(dept);
        return Result.success();
    }

    @PutMapping
    @AuditLog(operation = "UPDATE", module = "部门管理", description = "编辑部门")
    public Result<String> update(@Valid @RequestBody Dept dept) {
        Dept existing = deptService.getOne(new LambdaQueryWrapper<Dept>().eq(Dept::getCode, dept.getCode()));
        if (existing != null && !existing.getId().equals(dept.getId())) {
            return Result.error(400, "部门编码已被占用");
        }
        deptService.updateById(dept);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @AuditLog(operation = "DELETE", module = "部门管理", description = "删除部门")
    public Result<String> delete(@PathVariable Long id) {
        long childCount = deptService.count(new LambdaQueryWrapper<Dept>().eq(Dept::getParentId, id));
        if (childCount > 0) {
            return Result.error(400, "该部门下存在子部门，不允许删除");
        }
        deptService.removeById(id);
        return Result.success();
    }

    @GetMapping("/user/{userId}")
    public Result<List<Dept>> getDeptsByUserId(@PathVariable Long userId) {
        return Result.success(deptService.getDeptsByUserId(userId));
    }

    @GetMapping("/user/{userId}/ids")
    public Result<List<Long>> getUserDeptIds(@PathVariable Long userId) {
        return Result.success(deptService.getUserDeptIds(userId));
    }

    @PutMapping("/user/{userId}/assign")
    @AuditLog(operation = "ASSIGN_DEPT", module = "用户管理", description = "分配用户部门")
    public Result<String> assignDepts(@PathVariable Long userId, @RequestBody java.util.Map<String, List<Long>> body) {
        List<Long> deptIds = body.get("deptIds");
        deptService.assignDepts(userId, deptIds);
        return Result.success("部门分配成功");
    }
}
