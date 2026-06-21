package com.example.usermanager.controller;

import com.example.usermanager.base.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("登录成功 - 管理员账号")
    void testLogin_success_admin() throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "admin");
        loginData.put("password", TEST_PASSWORD);

        String response = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginData)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertNotNull(data);
        assertEquals("admin", data.path("username").asText());
        assertEquals("管理员", data.path("nickname").asText());
        assertNotNull(data.path("token").asText());
        assertNotNull(data.path("refreshToken").asText());
        assertTrue(data.path("token").asText().length() > 10);
        assertTrue(data.path("accessTokenExpiresIn").asLong() > 0);
        assertTrue(data.path("refreshTokenExpiresIn").asLong() > 0);

        JsonNode roles = data.path("roles");
        assertNotNull(roles);
        assertTrue(roles.isArray());
        assertTrue(roles.size() >= 1);

        JsonNode permissions = data.path("permissions");
        assertNotNull(permissions);
        assertTrue(permissions.isArray());
        assertTrue(permissions.size() >= 1);

        JsonNode roleCodes = data.path("roleCodes");
        assertNotNull(roleCodes);
        assertTrue(roleCodes.isArray());
        assertTrue(roleCodes.toString().contains("ADMIN"));
    }

    @Test
    @DisplayName("登录成功 - 普通用户账号")
    void testLogin_success_normalUser() throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "zhangsan");
        loginData.put("password", TEST_PASSWORD);

        String response = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginData)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertEquals("zhangsan", data.path("username").asText());
        assertEquals("张三", data.path("nickname").asText());

        JsonNode roleCodes = data.path("roleCodes");
        assertTrue(roleCodes.toString().contains("EDITOR"));
    }

    @Test
    @DisplayName("登录失败 - 用户名不存在")
    void testLogin_fail_userNotFound() throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "nonexistent");
        loginData.put("password", TEST_PASSWORD);

        String response = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginData)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10001, extractCode(response));
        assertTrue(extractMessage(response).contains("用户名不存在"));
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void testLogin_fail_wrongPassword() throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "admin");
        loginData.put("password", "wrongpassword");

        String response = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginData)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10002, extractCode(response));
        assertTrue(extractMessage(response).contains("密码错误"));
    }

    @Test
    @DisplayName("登录失败 - 用户名为空")
    void testLogin_fail_emptyUsername() throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "");
        loginData.put("password", TEST_PASSWORD);

        String response = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginData)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10001, extractCode(response));
    }

    @Test
    @DisplayName("登录失败 - 密码为空")
    void testLogin_fail_emptyPassword() throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "admin");
        loginData.put("password", "");

        String response = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginData)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10002, extractCode(response));
    }

    @Test
    @DisplayName("登录失败 - 用户名为null")
    void testLogin_fail_nullUsername() throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("password", TEST_PASSWORD);

        String response = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginData)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10001, extractCode(response));
    }

    @Test
    @DisplayName("访问需要认证的接口 - 不携带token被拒绝")
    void testAccessProtectedEndpoint_noToken_denied() throws Exception {
        mockMvc.perform(get("/api/user/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("访问需要认证的接口 - 携带无效token被拒绝")
    void testAccessProtectedEndpoint_invalidToken_denied() throws Exception {
        mockMvc.perform(get("/api/user/list")
                        .header("Authorization", "Bearer invalid.token.here")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("访问需要认证的接口 - 使用refresh token被拒绝")
    void testAccessProtectedEndpoint_refreshToken_denied() throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "admin");
        loginData.put("password", TEST_PASSWORD);

        String loginResponse = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginData)))
                .andReturn().getResponse().getContentAsString();

        String refreshToken = extractData(loginResponse).path("refreshToken").asText();

        mockMvc.perform(get("/api/user/list")
                        .header("Authorization", "Bearer " + refreshToken)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("访问需要认证的接口 - 携带有效token成功访问")
    void testAccessProtectedEndpoint_validToken_success() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        assertNotNull(extractData(response));
    }

    @Test
    @DisplayName("刷新令牌 - 正常路径")
    void testRefreshToken_success() throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "admin");
        loginData.put("password", TEST_PASSWORD);

        String loginResponse = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginData)))
                .andReturn().getResponse().getContentAsString();

        String refreshToken = extractData(loginResponse).path("refreshToken").asText();

        Map<String, String> refreshBody = new HashMap<>();
        refreshBody.put("refreshToken", refreshToken);

        String refreshResponse = mockMvc.perform(post("/api/user/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(refreshResponse));
        JsonNode data = extractData(refreshResponse);
        assertNotNull(data.path("token").asText());
        assertNotNull(data.path("refreshToken").asText());
        assertTrue(data.path("accessTokenExpiresIn").asLong() > 0);
        assertTrue(data.path("refreshTokenExpiresIn").asLong() > 0);
    }

    @Test
    @DisplayName("刷新令牌 - refresh token为空")
    void testRefreshToken_missingToken() throws Exception {
        Map<String, String> refreshBody = new HashMap<>();
        refreshBody.put("refreshToken", "");

        String response = mockMvc.perform(post("/api/user/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10010, extractCode(response));
        assertTrue(extractMessage(response).contains("刷新令牌缺失"));
    }

    @Test
    @DisplayName("刷新令牌 - refresh token无效")
    void testRefreshToken_invalidToken() throws Exception {
        Map<String, String> refreshBody = new HashMap<>();
        refreshBody.put("refreshToken", "invalid.refresh.token");

        String response = mockMvc.perform(post("/api/user/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10011, extractCode(response));
    }

    @Test
    @DisplayName("刷新令牌 - 使用access token刷新")
    void testRefreshToken_wrongTokenType() throws Exception {
        String accessToken = getAdminToken();

        Map<String, String> refreshBody = new HashMap<>();
        refreshBody.put("refreshToken", accessToken);

        String response = mockMvc.perform(post("/api/user/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10012, extractCode(response));
    }

    @Test
    @DisplayName("获取当前用户信息 - 正常路径")
    void testGetCurrentUserInfo_success() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/user/info")
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertEquals("admin", data.path("username").asText());
        assertEquals("管理员", data.path("nickname").asText());
        assertNotNull(data.path("roles"));
        assertNotNull(data.path("permissions"));
        assertNotNull(data.path("roleCodes"));
        assertNotNull(data.path("permissionCodes"));
    }

    @Test
    @DisplayName("访问登录接口 - 无需鉴权")
    void testLoginEndpoint_noAuthRequired() throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "admin");
        loginData.put("password", TEST_PASSWORD);

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginData)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("访问健康检查接口 - 无需鉴权")
    void testHealthEndpoint_noAuthRequired() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
