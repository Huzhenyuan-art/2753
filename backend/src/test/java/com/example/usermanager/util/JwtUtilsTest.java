package com.example.usermanager.util;

import com.example.usermanager.config.TestConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class JwtUtilsTest {

    @Autowired
    private JwtUtils jwtUtils;

    private static final String TEST_USERNAME = "testuser";
    private static final Long TEST_USER_ID = 100L;
    private static final List<String> TEST_ROLES = Arrays.asList("ADMIN", "USER");
    private static final List<String> TEST_PERMISSIONS = Arrays.asList("user:list", "user:add");

    @Test
    @DisplayName("生成访问令牌 - 正常路径")
    void testGenerateAccessToken_success() {
        String token = jwtUtils.generateAccessToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLES, TEST_PERMISSIONS);
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtUtils.validateToken(token));
        assertTrue(jwtUtils.isAccessToken(token));
        assertFalse(jwtUtils.isRefreshToken(token));
    }

    @Test
    @DisplayName("生成刷新令牌 - 正常路径")
    void testGenerateRefreshToken_success() {
        String token = jwtUtils.generateRefreshToken(TEST_USERNAME, TEST_USER_ID);
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtUtils.validateToken(token));
        assertTrue(jwtUtils.isRefreshToken(token));
        assertFalse(jwtUtils.isAccessToken(token));
    }

    @Test
    @DisplayName("从令牌中获取用户名")
    void testGetUsernameFromToken_success() {
        String token = jwtUtils.generateAccessToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLES, TEST_PERMISSIONS);
        String username = jwtUtils.getUsernameFromToken(token);
        assertEquals(TEST_USERNAME, username);
    }

    @Test
    @DisplayName("从令牌中获取用户ID")
    void testGetUserIdFromToken_success() {
        String token = jwtUtils.generateAccessToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLES, TEST_PERMISSIONS);
        Long userId = jwtUtils.getUserIdFromToken(token);
        assertEquals(TEST_USER_ID, userId);
    }

    @Test
    @DisplayName("从令牌中获取角色列表")
    void testGetRolesFromToken_success() {
        String token = jwtUtils.generateAccessToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLES, TEST_PERMISSIONS);
        List<String> roles = jwtUtils.getRolesFromToken(token);
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ADMIN"));
        assertTrue(roles.contains("USER"));
    }

    @Test
    @DisplayName("从令牌中获取权限列表")
    void testGetPermissionsFromToken_success() {
        String token = jwtUtils.generateAccessToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLES, TEST_PERMISSIONS);
        List<String> permissions = jwtUtils.getPermissionsFromToken(token);
        assertNotNull(permissions);
        assertEquals(2, permissions.size());
        assertTrue(permissions.contains("user:list"));
        assertTrue(permissions.contains("user:add"));
    }

    @Test
    @DisplayName("刷新令牌不包含角色和权限信息")
    void testRefreshToken_noRolesOrPermissions() {
        String token = jwtUtils.generateRefreshToken(TEST_USERNAME, TEST_USER_ID);
        List<String> roles = jwtUtils.getRolesFromToken(token);
        List<String> permissions = jwtUtils.getPermissionsFromToken(token);
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
        assertNotNull(permissions);
        assertTrue(permissions.isEmpty());
    }

    @Test
    @DisplayName("获取令牌过期时间")
    void testGetExpirationFromToken_success() {
        String token = jwtUtils.generateAccessToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLES, TEST_PERMISSIONS);
        Date expiration = jwtUtils.getExpirationFromToken(token);
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    @DisplayName("解析令牌claims")
    void testParseClaims_success() {
        String token = jwtUtils.generateAccessToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLES, TEST_PERMISSIONS);
        Claims claims = jwtUtils.parseClaims(token);
        assertNotNull(claims);
        assertEquals(TEST_USERNAME, claims.getSubject());
        assertEquals(JwtUtils.TOKEN_TYPE_ACCESS, claims.get(JwtUtils.CLAIM_TOKEN_TYPE));
    }

    @Test
    @DisplayName("验证有效令牌")
    void testValidateToken_validToken() {
        String token = jwtUtils.generateAccessToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLES, TEST_PERMISSIONS);
        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    @DisplayName("验证无效令牌 - 篡改签名")
    void testValidateToken_invalidSignature() {
        String validToken = jwtUtils.generateAccessToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLES, TEST_PERMISSIONS);
        String invalidToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";
        assertFalse(jwtUtils.validateToken(invalidToken));
    }

    @Test
    @DisplayName("验证无效令牌 - 空字符串")
    void testValidateToken_emptyString() {
        assertFalse(jwtUtils.validateToken(""));
    }

    @Test
    @DisplayName("验证无效令牌 - null")
    void testValidateToken_null() {
        assertFalse(jwtUtils.validateToken(null));
    }

    @Test
    @DisplayName("验证无效令牌 - 随机字符串")
    void testValidateToken_randomString() {
        assertFalse(jwtUtils.validateToken("not-a-valid-jwt-token"));
    }

    @Test
    @DisplayName("验证令牌类型识别")
    void testTokenTypeDetection() {
        String accessToken = jwtUtils.generateAccessToken(TEST_USERNAME, TEST_USER_ID, TEST_ROLES, TEST_PERMISSIONS);
        String refreshToken = jwtUtils.generateRefreshToken(TEST_USERNAME, TEST_USER_ID);

        assertTrue(jwtUtils.isAccessToken(accessToken));
        assertFalse(jwtUtils.isRefreshToken(accessToken));
        assertEquals(JwtUtils.TOKEN_TYPE_ACCESS, jwtUtils.getTokenType(accessToken));

        assertTrue(jwtUtils.isRefreshToken(refreshToken));
        assertFalse(jwtUtils.isAccessToken(refreshToken));
        assertEquals(JwtUtils.TOKEN_TYPE_REFRESH, jwtUtils.getTokenType(refreshToken));
    }

    @Test
    @DisplayName("获取访问令牌过期时间配置")
    void testGetAccessTokenExpiration() {
        assertTrue(jwtUtils.getAccessTokenExpiration() > 0);
    }

    @Test
    @DisplayName("获取刷新令牌过期时间配置")
    void testGetRefreshTokenExpiration() {
        assertTrue(jwtUtils.getRefreshTokenExpiration() > 0);
    }

    @Test
    @DisplayName("空角色列表生成令牌")
    void testGenerateToken_emptyRoles() {
        String token = jwtUtils.generateAccessToken(TEST_USERNAME, TEST_USER_ID, null, null);
        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));
        List<String> roles = jwtUtils.getRolesFromToken(token);
        List<String> permissions = jwtUtils.getPermissionsFromToken(token);
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
        assertNotNull(permissions);
        assertTrue(permissions.isEmpty());
    }
}
