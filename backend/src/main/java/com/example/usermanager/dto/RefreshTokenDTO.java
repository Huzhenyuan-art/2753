package com.example.usermanager.dto;

import com.example.usermanager.entity.Dept;
import lombok.Data;

import java.util.List;

@Data
public class RefreshTokenDTO {
    private String token;
    private String refreshToken;
    private Long accessTokenExpiresIn;
    private Long refreshTokenExpiresIn;
    private List<Dept> depts;
}
