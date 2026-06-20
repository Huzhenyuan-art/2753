package com.example.usermanager.dto;

import lombok.Data;

@Data
public class RefreshTokenDTO {
    private String token;
    private String refreshToken;
    private Long accessTokenExpiresIn;
    private Long refreshTokenExpiresIn;
}
