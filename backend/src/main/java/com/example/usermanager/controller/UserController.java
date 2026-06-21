package com.example.usermanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanager.annotation.AuditLog;
import com.example.usermanager.common.Result;
import com.example.usermanager.dto.ChangePasswordDTO;
import com.example.usermanager.dto.LoginUserDTO;
import com.example.usermanager.dto.RefreshTokenDTO;
import com.example.usermanager.dto.UpdateProfileDTO;
import com.example.usermanager.dto.UserImportResult;
import com.example.usermanager.entity.Dept;
import com.example.usermanager.entity.Permission;
import com.example.usermanager.entity.Role;
import com.example.usermanager.entity.User;
import com.example.usermanager.service.DeptService;
import com.example.usermanager.service.PermissionService;
import com.example.usermanager.service.RoleService;
import com.example.usermanager.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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
    private DeptService deptService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/login")
    @AuditLog(operation = "LOGIN", module = "用户管理", description = "用户登录", recordParams = false, recordResult = false)
    public Result<LoginUserDTO> login(@RequestBody Map<String, String> loginData) {
        LoginUserDTO loginUser = userService.login(loginData.get("username"), loginData.get("password"));
        return Result.success(loginUser);
    }

    @PostMapping("/refresh")
    @AuditLog(operation = "REFRESH_TOKEN", module = "用户管理", description = "刷新访问令牌", recordParams = false, recordResult = false)
    public Result<RefreshTokenDTO> refreshToken(@RequestBody Map<String, String> body) {
        RefreshTokenDTO dto = userService.refreshToken(body.get("refreshToken"));
        return Result.success(dto);
    }

    @GetMapping("/list")
    public Result<Page<User>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                 @RequestParam(required = false) String username,
                                 @RequestParam(required = false) Integer status,
                                 @RequestParam(required = false) Long deptId,
                                 @RequestParam(required = false) String sortField,
                                 @RequestParam(required = false) String sortOrder) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = buildUserQueryWrapper(username, status, sortField, sortOrder);
        return Result.success(userService.pageWithDept(page, wrapper, deptId));
    }

    @PutMapping("/{id}/status")
    @AuditLog(operation = "STATUS", module = "用户管理", description = "切换用户状态")
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
    @AuditLog(operation = "CREATE", module = "用户管理", description = "新增用户")
    public Result<Long> add(@Valid @RequestBody User user) {
        if (userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) != null) {
            return Result.error(400, "用户名 '" + user.getUsername() + "' 已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.save(user);
        if (user.getDeptIds() != null && !user.getDeptIds().isEmpty()) {
            deptService.assignDepts(user.getId(), user.getDeptIds());
        }
        return Result.success(user.getId());
    }

    @PutMapping
    @AuditLog(operation = "UPDATE", module = "用户管理", description = "编辑用户")
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
        if (user.getDeptIds() != null) {
            deptService.assignDepts(user.getId(), user.getDeptIds());
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @AuditLog(operation = "DELETE", module = "用户管理", description = "删除用户")
    public Result<String> delete(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null && "admin".equals(user.getUsername())) {
            return Result.error(403, "管理员账号不允许删除");
        }
        userService.removeById(id);
        return Result.success();
    }

    @PutMapping("/change-password")
    @AuditLog(operation = "CHANGE_PASSWORD", module = "用户管理", description = "修改密码", recordParams = false, recordResult = false)
    public Result<String> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.changePassword(username, dto);
        return Result.success("密码修改成功，请重新登录");
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
        List<Dept> depts = deptService.getDeptsByUserId(user.getId());

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
        dto.setDepts(depts);
        return Result.success(dto);
    }

    @PutMapping("/profile")
    @AuditLog(operation = "UPDATE_PROFILE", module = "用户管理", description = "修改个人资料", recordParams = false)
    public Result<String> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        userService.updateById(user);
        return Result.success("个人资料更新成功");
    }

    @GetMapping("/{id}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long id) {
        return Result.success(userService.getUserRoleIds(id));
    }

    @PutMapping("/{id}/roles")
    @AuditLog(operation = "ASSIGN_ROLE", module = "用户管理", description = "分配用户角色")
    public Result<String> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        List<Long> roleIds = body.get("roleIds");
        userService.assignRoles(id, roleIds);
        return Result.success("角色分配成功");
    }

    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam String username,
                                         @RequestParam(required = false) Long excludeId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(User::getId, excludeId);
        }
        long count = userService.count(wrapper);
        return Result.success(count == 0);
    }

    @GetMapping("/template")
    @AuditLog(operation = "DOWNLOAD_TEMPLATE", module = "用户管理", description = "下载用户导入模板")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        writeExcelResponse(response, "用户导入模板", outputStream -> {
            userService.downloadTemplate(outputStream);
        });
    }

    @PostMapping("/import")
    @AuditLog(operation = "IMPORT", module = "用户管理", description = "批量导入用户")
    public Result<UserImportResult> importUsers(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择要导入的文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".xlsx")
                && !originalFilename.toLowerCase().endsWith(".xls"))) {
            return Result.error(400, "仅支持 Excel 文件格式（.xlsx 或 .xls）");
        }
        UserImportResult result = userService.importUsers(file);
        return Result.success(result);
    }

    @GetMapping("/export")
    @AuditLog(operation = "EXPORT", module = "用户管理", description = "导出用户列表")
    public void exportUsers(@RequestParam(required = false) String username,
                            @RequestParam(required = false) Integer status,
                            @RequestParam(required = false) Long deptId,
                            @RequestParam(required = false) String sortField,
                            @RequestParam(required = false) String sortOrder,
                            HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<User> wrapper = buildUserQueryWrapper(username, status, sortField, sortOrder);
        writeExcelResponse(response, "用户列表", outputStream -> {
            userService.exportUsers(outputStream, wrapper, deptId);
        });
    }

    private LambdaQueryWrapper<User> buildUserQueryWrapper(String username, Integer status,
                                                            String sortField, String sortOrder) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username).or().like(User::getNickname, username);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (StringUtils.hasText(sortField) && StringUtils.hasText(sortOrder)) {
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
            if ("username".equals(sortField)) {
                wrapper.orderBy(true, isAsc, User::getUsername);
            } else if ("createTime".equals(sortField)) {
                wrapper.orderBy(true, isAsc, User::getCreateTime);
            } else {
                wrapper.orderByDesc(User::getCreateTime);
            }
        } else {
            wrapper.orderByDesc(User::getCreateTime);
        }
        return wrapper;
    }

    @FunctionalInterface
    private interface ExcelWriter {
        void write(ServletOutputStream outputStream) throws Exception;
    }

    private void writeExcelResponse(HttpServletResponse response, String fileName, ExcelWriter writer) throws IOException {
        ServletOutputStream outputStream = null;
        try {
            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
            outputStream = response.getOutputStream();
            writer.write(outputStream);
            outputStream.flush();
        } catch (Exception e) {
            log.error("{}失败: ", fileName, e);
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException ignored) {}
            }
            response.reset();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("code", 500);
            result.put("message", fileName + "失败：" + (e.getMessage() != null ? e.getMessage() : "系统内部错误"));
            result.put("data", null);
            response.getWriter().write(objectMapper.writeValueAsString(result));
        } finally {
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException ignored) {}
            }
        }
    }
}
