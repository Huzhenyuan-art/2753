package com.example.usermanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanager.common.Result;
import com.example.usermanager.dto.LoginUserDTO;
import com.example.usermanager.entity.Permission;
import com.example.usermanager.entity.Role;
import com.example.usermanager.entity.User;
import com.example.usermanager.service.PermissionService;
import com.example.usermanager.service.RoleService;
import com.example.usermanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<LoginUserDTO> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        try {
            LoginUserDTO loginUser = userService.login(username, password);
            return Result.success(loginUser);
        } catch (RuntimeException e) {
            String errorCode = e.getMessage();
            switch (errorCode) {
                case "USER_NOT_FOUND":
                    return Result.error(10001, "用户名不存在");
                case "PASSWORD_ERROR":
                    return Result.error(10002, "密码错误");
                case "ACCOUNT_DISABLED":
                    return Result.error(10003, "账号已被禁用，请联系管理员");
                default:
                    return Result.error(401, "登录失败，请重试");
            }
        }
    }

    @GetMapping("/list")
    public Result<Page<User>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                 @RequestParam(required = false) String username,
                                 @RequestParam(required = false) Integer status) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username).or().like(User::getNickname, username);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);
        return Result.success(userService.page(page, wrapper));
    }

    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        if ("admin".equals(user.getUsername())) {
            return Result.error(403, "管理员账号不允许禁用");
        }
        if (status != 0 && status != 1) {
            return Result.error(400, "状态值不合法，只能为 0 或 1");
        }
        user.setStatus(status);
        userService.updateById(user);
        return Result.success(status == 1 ? "用户已启用" : "用户已禁用");
    }

    @PostMapping
    public Result<String> add(@Valid @RequestBody User user) {
        if (userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) != null) {
            return Result.error(400, "用户名 '" + user.getUsername() + "' 已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.save(user);
        return Result.success();
    }

    @PutMapping
    public Result<String> update(@Valid @RequestBody User user) {
        User existingUser = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        if (existingUser != null && !existingUser.getId().equals(user.getId())) {
            return Result.error(400, "用户名 '" + user.getUsername() + "' 已被占用");
        }
        
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userService.updateById(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null && "admin".equals(user.getUsername())) {
            return Result.error(403, "管理员账号不允许删除");
        }
        userService.removeById(id);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<LoginUserDTO> getCurrentUserInfo() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        List<Role> roles = roleService.getRolesByUserId(user.getId());
        List<Permission> permissions = permissionService.getPermissionsByUserId(user.getId());
        List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());
        List<String> permissionCodes = permissions.stream().map(Permission::getCode).collect(Collectors.toList());

        LoginUserDTO dto = new LoginUserDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setStatus(user.getStatus());
        dto.setCreateTime(user.getCreateTime());
        dto.setUpdateTime(user.getUpdateTime());
        dto.setRoles(roles);
        dto.setPermissions(permissions);
        dto.setRoleCodes(roleCodes);
        dto.setPermissionCodes(permissionCodes);
        return Result.success(dto);
    }

    @GetMapping("/{id}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long id) {
        return Result.success(userService.getUserRoleIds(id));
    }

    @PutMapping("/{id}/roles")
    public Result<String> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        List<Long> roleIds = body.get("roleIds");
        userService.assignRoles(id, roleIds);
        return Result.success("角色分配成功");
    }
}
