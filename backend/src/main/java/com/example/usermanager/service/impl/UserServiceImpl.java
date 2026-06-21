package com.example.usermanager.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.usermanager.dto.ChangePasswordDTO;
import com.example.usermanager.dto.ImportErrorItem;
import com.example.usermanager.dto.LoginUserDTO;
import com.example.usermanager.dto.RefreshTokenDTO;
import com.example.usermanager.dto.UserExcelDTO;
import com.example.usermanager.dto.UserExportDTO;
import com.example.usermanager.dto.UserImportResult;
import com.example.usermanager.entity.Dept;
import com.example.usermanager.entity.Permission;
import com.example.usermanager.entity.Role;
import com.example.usermanager.entity.User;
import com.example.usermanager.entity.UserDept;
import com.example.usermanager.entity.UserRole;
import com.example.usermanager.exception.BusinessException;
import com.example.usermanager.exception.ErrorCode;
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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
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

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    @Override
    public LoginUserDTO login(String username, String password) {
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        List<Role> roles = roleService.getRolesByUserId(user.getId());
        List<Permission> permissions = permissionService.getPermissionsByUserId(user.getId());
        List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());
        List<String> permissionCodes = permissions.stream().map(Permission::getCode).collect(Collectors.toList());
        List<Dept> depts = deptService.getDeptsByUserId(user.getId());

        String accessToken = jwtUtils.generateAccessToken(username, user.getId(), roleCodes, permissionCodes);
        String refreshToken = jwtUtils.generateRefreshToken(username, user.getId());

        LoginUserDTO dto = new LoginUserDTO();
        dto.setToken(accessToken);
        dto.setRefreshToken(refreshToken);
        dto.setAccessTokenExpiresIn(jwtUtils.getAccessTokenExpiration());
        dto.setRefreshTokenExpiresIn(jwtUtils.getRefreshTokenExpiration());
        populateUserInfo(dto, user, roles, permissions, roleCodes, permissionCodes, depts);
        return dto;
    }

    @Override
    public RefreshTokenDTO refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISSING);
        }

        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        if (!jwtUtils.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_TYPE_ERROR);
        }

        String username = jwtUtils.getUsernameFromToken(refreshToken);
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);

        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ErrorCode.REFRESH_USER_NOT_FOUND);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.REFRESH_ACCOUNT_DISABLED);
        }
        if (!user.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        validateTokenNotBeforePasswordChange(refreshToken, user.getPasswordChangedAt());

        List<Role> roles = roleService.getRolesByUserId(user.getId());
        List<Permission> permissions = permissionService.getPermissionsByUserId(user.getId());
        List<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toList());
        List<String> permissionCodes = permissions.stream().map(Permission::getCode).collect(Collectors.toList());
        List<Dept> depts = deptService.getDeptsByUserId(user.getId());

        String newAccessToken = jwtUtils.generateAccessToken(username, user.getId(), roleCodes, permissionCodes);
        String newRefreshToken = jwtUtils.generateRefreshToken(username, user.getId());

        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setToken(newAccessToken);
        dto.setRefreshToken(newRefreshToken);
        dto.setAccessTokenExpiresIn(jwtUtils.getAccessTokenExpiration());
        dto.setRefreshTokenExpiresIn(jwtUtils.getRefreshTokenExpiration());
        dto.setDepts(depts);
        return dto;
    }

    @Override
    public void changePassword(String username, ChangePasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.CONFIRM_PASSWORD_MISMATCH);
        }
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR);
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.SAME_AS_OLD_PASSWORD);
        }
        if (WEAK_PASSWORDS.contains(dto.getNewPassword().toLowerCase())) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD);
        }
        if (countPasswordComplexity(dto.getNewPassword()) < 2) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_COMPLEXITY);
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        this.updateById(user);
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
        applyDeptFilter(wrapper, deptId);
        Page<User> resultPage = this.page(page, wrapper);
        fillUserDepts(resultPage.getRecords());
        return resultPage;
    }

    @Override
    public void downloadTemplate(OutputStream outputStream) {
        List<UserExcelDTO> demoData = new ArrayList<>();
        UserExcelDTO demo1 = new UserExcelDTO();
        demo1.setUsername("zhangsan");
        demo1.setPassword("123456");
        demo1.setNickname("张三");
        demo1.setEmail("zhangsan@example.com");
        demo1.setStatusText("1");
        demoData.add(demo1);

        UserExcelDTO demo2 = new UserExcelDTO();
        demo2.setUsername("lisi");
        demo2.setPassword("123456");
        demo2.setNickname("李四");
        demo2.setEmail("lisi@example.com");
        demo2.setStatusText("0");
        demoData.add(demo2);

        EasyExcel.write(outputStream, UserExcelDTO.class)
                .sheet("用户导入模板")
                .doWrite(demoData);
    }

    @Override
    @Transactional
    public UserImportResult importUsers(MultipartFile file) {
        List<UserExcelDTO> excelData = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), UserExcelDTO.class, new ReadListener<UserExcelDTO>() {
                @Override
                public void invoke(UserExcelDTO data, AnalysisContext context) {
                    excelData.add(data);
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new RuntimeException("读取Excel文件失败: " + e.getMessage());
        }

        int totalCount = excelData.size();
        int successCount = 0;
        List<ImportErrorItem> errors = new ArrayList<>();
        Set<String> fileUsernames = new HashSet<>();

        List<User> dbExistingUsers = this.list(new LambdaQueryWrapper<User>().select(User::getUsername));
        Set<String> dbUsernames = dbExistingUsers.stream()
                .map(User::getUsername)
                .collect(Collectors.toSet());

        List<User> toSaveUsers = new ArrayList<>();

        for (int i = 0; i < excelData.size(); i++) {
            int rowNum = i + 2;
            UserExcelDTO dto = excelData.get(i);
            List<String> rowErrors = new ArrayList<>();
            String rowUsername = dto.getUsername();

            if (!StringUtils.hasText(rowUsername)) {
                rowErrors.add("用户名不能为空");
            } else {
                if (fileUsernames.contains(rowUsername)) {
                    rowErrors.add("文件内存在重复用户名");
                }
                if (dbUsernames.contains(rowUsername)) {
                    rowErrors.add("用户名已存在数据库中");
                }
                fileUsernames.add(rowUsername);
            }

            if (!StringUtils.hasText(dto.getPassword())) {
                rowErrors.add("密码不能为空");
            } else if (dto.getPassword().length() < 6) {
                rowErrors.add("密码长度至少为6位");
            }

            if (!StringUtils.hasText(dto.getNickname())) {
                rowErrors.add("昵称不能为空");
            }

            String email = dto.getEmail();
            if (StringUtils.hasText(email)) {
                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    rowErrors.add("邮箱格式不正确");
                }
            }

            Integer statusValue = null;
            if (StringUtils.hasText(dto.getStatusText())) {
                String text = dto.getStatusText().trim();
                if ("0".equals(text) || "1".equals(text)) {
                    statusValue = Integer.parseInt(text);
                } else {
                    rowErrors.add("状态值只能为 0(禁用) 或 1(启用)");
                }
            }

            if (!rowErrors.isEmpty()) {
                errors.add(new ImportErrorItem(rowNum, rowUsername, String.join("；", rowErrors)));
                continue;
            }

            User user = new User();
            user.setUsername(rowUsername);
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setNickname(dto.getNickname());
            user.setEmail(email);
            user.setStatus(statusValue != null ? statusValue : 1);
            toSaveUsers.add(user);
            dbUsernames.add(rowUsername);
            successCount++;
        }

        if (!toSaveUsers.isEmpty()) {
            this.saveBatch(toSaveUsers);
        }

        int failCount = totalCount - successCount;
        return new UserImportResult(totalCount, successCount, failCount, errors);
    }

    @Override
    public void exportUsers(OutputStream outputStream, LambdaQueryWrapper<User> wrapper, Long deptId) {
        applyDeptFilter(wrapper, deptId);
        List<User> users = this.list(wrapper);

        List<UserExportDTO> exportList = new ArrayList<>();
        if (!users.isEmpty()) {
            List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
            List<UserDept> allUserDepts = userIds.isEmpty() ? new ArrayList<>() :
                    userDeptMapper.selectList(new LambdaQueryWrapper<UserDept>().in(UserDept::getUserId, userIds));
            List<Long> allDeptIds = allUserDepts.stream()
                    .map(UserDept::getDeptId)
                    .distinct()
                    .collect(Collectors.toList());
            List<Dept> allDepts = allDeptIds.isEmpty() ? new ArrayList<>() : deptService.listByIds(allDeptIds);
            Map<Long, String> deptNameMap = allDepts.stream()
                    .collect(Collectors.toMap(Dept::getId, Dept::getName));
            Map<Long, List<Dept>> userDeptMap = allUserDepts.stream()
                    .collect(Collectors.groupingBy(
                            UserDept::getUserId,
                            Collectors.mapping(ud -> {
                                Dept d = new Dept();
                                d.setId(ud.getDeptId());
                                d.setName(deptNameMap.get(ud.getDeptId()));
                                return d;
                            }, Collectors.toList())
                    ));

            for (User user : users) {
                UserExportDTO dto = new UserExportDTO();
                dto.setId(user.getId());
                dto.setUsername(user.getUsername());
                dto.setNickname(user.getNickname());
                dto.setEmail(user.getEmail());
                dto.setStatusText(user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用");
                List<Dept> userDeptsList = userDeptMap.getOrDefault(user.getId(), new ArrayList<>());
                String deptNames = userDeptsList.stream()
                        .map(Dept::getName)
                        .filter(name -> name != null)
                        .collect(Collectors.joining("、"));
                dto.setDeptNames(deptNames);
                dto.setCreateTime(user.getCreateTime() != null ? user.getCreateTime().format(DATE_TIME_FORMATTER) : "");
                dto.setUpdateTime(user.getUpdateTime() != null ? user.getUpdateTime().format(DATE_TIME_FORMATTER) : "");
                exportList.add(dto);
            }
        }

        EasyExcel.write(outputStream, UserExportDTO.class)
                .sheet("用户列表")
                .doWrite(exportList);
    }

    private void populateUserInfo(LoginUserDTO dto, User user, List<Role> roles,
                                  List<Permission> permissions, List<String> roleCodes,
                                  List<String> permissionCodes, List<Dept> depts) {
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
    }

    private void applyDeptFilter(LambdaQueryWrapper<User> wrapper, Long deptId) {
        if (deptId == null) {
            return;
        }
        List<Long> deptIds = deptService.getChildDeptIds(deptId);
        if (deptIds == null || deptIds.isEmpty()) {
            wrapper.eq(User::getId, -1L);
            return;
        }
        List<UserDept> userDepts = userDeptMapper.selectList(
                new LambdaQueryWrapper<UserDept>().in(UserDept::getDeptId, deptIds));
        if (userDepts == null || userDepts.isEmpty()) {
            wrapper.eq(User::getId, -1L);
            return;
        }
        List<Long> userIds = userDepts.stream()
                .map(UserDept::getUserId)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            wrapper.eq(User::getId, -1L);
        } else {
            wrapper.in(User::getId, userIds);
        }
    }

    private void fillUserDepts(List<User> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream().map(User::getId).collect(Collectors.toList());
        List<UserDept> allUserDepts = userDeptMapper.selectList(
                new LambdaQueryWrapper<UserDept>().in(UserDept::getUserId, userIds));
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

    private void validateTokenNotBeforePasswordChange(String token, LocalDateTime passwordChangedAt) {
        if (passwordChangedAt == null) {
            return;
        }
        java.util.Date tokenIssuedAt = jwtUtils.parseClaims(token).getIssuedAt();
        if (tokenIssuedAt == null) {
            return;
        }
        LocalDateTime tokenIssuedDateTime = tokenIssuedAt.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        if (tokenIssuedDateTime.isBefore(passwordChangedAt)) {
            throw new BusinessException(ErrorCode.PASSWORD_CHANGED);
        }
    }

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
}
