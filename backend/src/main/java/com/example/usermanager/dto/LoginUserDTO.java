package com.example.usermanager.dto;

import com.example.usermanager.entity.Permission;
import com.example.usermanager.entity.Role;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LoginUserDTO {
    private String token;
    private String refreshToken;
    private Long accessTokenExpiresIn;
    private Long refreshTokenExpiresIn;
    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<Role> roles;
    private List<Permission> permissions;
    private List<String> roleCodes;
    private List<String> permissionCodes;
}
