package com.example.usermanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.usermanager.dto.ChangePasswordDTO;
import com.example.usermanager.dto.LoginUserDTO;
import com.example.usermanager.dto.RefreshTokenDTO;
import com.example.usermanager.entity.Dept;
import com.example.usermanager.entity.Permission;
import com.example.usermanager.entity.Role;
import com.example.usermanager.entity.User;
import com.example.usermanager.entity.UserDept;
import com.example.usermanager.entity.UserRole;
import com.example.usermanager.mapper.UserDeptMapper;
import com.example.usermanager.mapper.UserMapper;
import com.example.usermanager.mapper.UserRoleMapper;
import com.example.usermanager.service.DeptService;
import com.example.usermanager.service.PermissionService;
import com.example.usermanager.service.RoleService;
import com.example.usermanager.service.UserService;
import com.example.usermanager.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserDeptMapper userDeptMapper;

    @Autowired
    @Lazy
    private DeptService deptService;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Lazy
    private JwtUtils jwtUtils;

    @Autowired
    @Lazy
    private RoleService roleService;

    @Autowired
    @Lazy
    private PermissionService permissionService;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Override
    public LoginUserDTO login(String username, String password) {
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new RuntimeException("USER_NOT_FOUND");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("PASSWORD_ERROR");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("ACCOUNT_DISABLED");
        }

        List<Role> roles = roleService.getRolesByUserId(user.getId());
        List<Permission> permissions = permissionService.getPermissionsByUserId(user.getId());

        List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());
        List<String> permissionCodes = permissions.stream().map(Permission::getCode).collect(Collectors.toList());

        String accessToken = jwtUtils.generateAccessToken(username, user.getId(), roleCodes, permissionCodes);
        String refreshToken = jwtUtils.generateRefreshToken(username, user.getId());

        LoginUserDTO dto = new LoginUserDTO();
        dto.setToken(accessToken);
        dto.setRefreshToken(refreshToken);
        dto.setAccessTokenExpiresIn(jwtUtils.getAccessTokenExpiration());
        dto.setRefreshTokenExpiresIn(jwtUtils.getRefreshTokenExpiration());
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

        List<Dept> depts = deptService.getDeptsByUserId(user.getId());
        dto.setDepts(depts);

        return dto;
    }

    @Override
    public RefreshTokenDTO refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new RuntimeException("REFRESH_TOKEN_MISSING");
        }

        if (!jwtUtils.validateToken(refreshToken)) {
            throw new RuntimeException("REFRESH_TOKEN_INVALID");
        }

        if (!jwtUtils.isRefreshToken(refreshToken)) {
            throw new RuntimeException("REFRESH_TOKEN_TYPE_ERROR");
        }

        String username = jwtUtils.getUsernameFromToken(refreshToken);
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);

        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new RuntimeException("USER_NOT_FOUND");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("ACCOUNT_DISABLED");
        }
        if (!user.getId().equals(userId)) {
            throw new RuntimeException("REFRESH_TOKEN_INVALID");
        }

        if (user.getPasswordChangedAt() != null) {
            java.time.LocalDateTime passwordChangedAt = user.getPasswordChangedAt();
            java.util.Date tokenIssuedAt = jwtUtils.parseClaims(refreshToken).getIssuedAt();
            if (tokenIssuedAt != null) {
                java.time.LocalDateTime tokenIssuedDateTime = tokenIssuedAt.toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                if (tokenIssuedDateTime.isBefore(passwordChangedAt)) {
                    throw new RuntimeException("PASSWORD_CHANGED");
                }
            }
        }

        List<Role> roles = roleService.getRolesByUserId(user.getId());
        List<Permission> permissions = permissionService.getPermissionsByUserId(user.getId());
        List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());
        List<String> permissionCodes = permissions.stream().map(Permission::getCode).collect(Collectors.toList());

        String newAccessToken = jwtUtils.generateAccessToken(username, user.getId(), roleCodes, permissionCodes);
        String newRefreshToken = jwtUtils.generateRefreshToken(username, user.getId());

        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setToken(newAccessToken);
        dto.setRefreshToken(newRefreshToken);
        dto.setAccessTokenExpiresIn(jwtUtils.getAccessTokenExpiration());
        dto.setRefreshTokenExpiresIn(jwtUtils.getRefreshTokenExpiration());

        List<Dept> depts = deptService.getDeptsByUserId(user.getId());
        dto.setDepts(depts);

        return dto;
    }

    @Override
    public void changePassword(String username, ChangePasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("CONFIRM_PASSWORD_MISMATCH");
        }
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new RuntimeException("USER_NOT_FOUND");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("OLD_PASSWORD_ERROR");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("SAME_AS_OLD_PASSWORD");
        }
        if (WEAK_PASSWORDS.contains(dto.getNewPassword().toLowerCase())) {
            throw new RuntimeException("WEAK_PASSWORD");
        }
        if (countPasswordComplexity(dto.getNewPassword()) < 2) {
            throw new RuntimeException("INSUFFICIENT_COMPLEXITY");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        this.updateById(user);
    }

    private static final Set<String> WEAK_PASSWORDS = new HashSet<>(Arrays.asList(
            "password", "password1", "password123", "password@123",
            "123456", "12345678", "123456789", "1234567890",
            "123123", "123321", "111111", "000000",
            "654321", "88888888", "666666",
            "admin", "admin123", "admin@123",
            "qwerty", "qwerty123", "qwertyuiop",
            "letmein", "welcome", "iloveyou",
            "abc123", "abc@123",
            "user@123", "test@123",
            "pass@123", "pass@word1",
            "1q2w3e4r", "1qaz2wsx",
            "p@ssw0rd", "p@ssword"
    ));

    private int countPasswordComplexity(String password) {
        int count = 0;
        if (password.matches(".*[A-Za-z].*")) {
            count++;
        }
        if (password.matches(".*\\d.*")) {
            count++;
        }
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?].*")) {
            count++;
        }
        return count;
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (userRoles == null || userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        return userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
    }

    @Override
    public Page<User> pageWithDept(Page<User> page, LambdaQueryWrapper<User> wrapper, Long deptId) {
        if (deptId != null) {
            List<Long> deptIds = deptService.getChildDeptIds(deptId);
            if (deptIds != null && !deptIds.isEmpty()) {
                List<UserDept> userDepts = userDeptMapper.selectList(
                        new LambdaQueryWrapper<UserDept>().in(UserDept::getDeptId, deptIds));
                if (userDepts != null && !userDepts.isEmpty()) {
                    List<Long> userIds = userDepts.stream()
                            .map(UserDept::getUserId)
                            .distinct()
                            .collect(Collectors.toList());
                    if (!userIds.isEmpty()) {
                        wrapper.in(User::getId, userIds);
                    } else {
                        wrapper.eq(User::getId, -1L);
                    }
                } else {
                    wrapper.eq(User::getId, -1L);
                }
            } else {
                wrapper.eq(User::getId, -1L);
            }
        }

        Page<User> resultPage = this.page(page, wrapper);
        List<User> records = resultPage.getRecords();
        if (records != null && !records.isEmpty()) {
            List<Long> userIds = records.stream().map(User::getId).collect(Collectors.toList());
            List<UserDept> allUserDepts;
            if (userIds.isEmpty()) {
                allUserDepts = new ArrayList<>();
            } else {
                allUserDepts = userDeptMapper.selectList(
                        new LambdaQueryWrapper<UserDept>().in(UserDept::getUserId, userIds));
            }
            List<Long> allDeptIds = allUserDepts.stream()
                    .map(UserDept::getDeptId)
                    .distinct()
                    .collect(Collectors.toList());
            List<Dept> allDepts = deptService.listByIds(allDeptIds);
            Map<Long, Dept> deptMap = allDepts.stream()
                    .collect(Collectors.toMap(Dept::getId, dept -> dept));
            Map<Long, List<Dept>> userDeptMap = allUserDepts.stream()
                    .collect(Collectors.groupingBy(
                            UserDept::getUserId,
                            Collectors.mapping(
                                    ud -> deptMap.get(ud.getDeptId()),
                                    Collectors.toList()
                            )
                    ));
            for (User user : records) {
                user.setDepts(userDeptMap.getOrDefault(user.getId(), new ArrayList<>()));
            }
        }
        return resultPage;
    }
}
