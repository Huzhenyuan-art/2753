package com.example.usermanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanager.common.Result;
import com.example.usermanager.entity.User;
import com.example.usermanager.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        String token = userService.login(username, password);
        if (token != null) {
            return Result.success(token);
        }
        return Result.error(401, "用户名或密码错误");
    }

    @GetMapping("/list")
    public Result<Page<User>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                 @RequestParam(required = false) String username) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username).or().like(User::getNickname, username);
        }
        wrapper.orderByDesc(User::getCreateTime);
        return Result.success(userService.page(page, wrapper));
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
            user.setPassword(null); // Don't update password if not provided
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
    public Result<User> getCurrentUserInfo() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
}
