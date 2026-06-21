package com.example.usermanager.controller;

import com.example.usermanager.base.AbstractIntegrationTest;
import com.example.usermanager.dto.ChangePasswordDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PasswordChangeIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("修改密码成功 - 符合复杂度要求")
    void testChangePassword_success() throws Exception {
        String token = getZhangsanToken();

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(TEST_PASSWORD);
        dto.setNewPassword("NewPass@123");
        dto.setConfirmPassword("NewPass@123");

        String response = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        assertTrue(extractMessage(response).contains("密码修改成功"));

        String newToken = loginAndGetToken("zhangsan", "NewPass@123");
        assertNotNull(newToken);

        ChangePasswordDTO revertDto = new ChangePasswordDTO();
        revertDto.setOldPassword("NewPass@123");
        revertDto.setNewPassword(STRONG_PASSWORD);
        revertDto.setConfirmPassword(STRONG_PASSWORD);

        String revertResponse = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(newToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revertDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(revertResponse));

        String revertBackToken = loginAndGetToken("zhangsan", STRONG_PASSWORD);
        ChangePasswordDTO resetDto = new ChangePasswordDTO();
        resetDto.setOldPassword(STRONG_PASSWORD);
        resetDto.setNewPassword(TEST_PASSWORD);
        resetDto.setConfirmPassword(TEST_PASSWORD);

        mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(revertBackToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("修改密码失败 - 旧密码错误")
    void testChangePassword_fail_wrongOldPassword() throws Exception {
        String token = getZhangsanToken();

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("wrongpassword");
        dto.setNewPassword("NewPass@123");
        dto.setConfirmPassword("NewPass@123");

        String response = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10004, extractCode(response));
        assertTrue(extractMessage(response).contains("旧密码错误"));
    }

    @Test
    @DisplayName("修改密码失败 - 新密码与旧密码相同")
    void testChangePassword_fail_sameAsOldPassword() throws Exception {
        String token = getZhangsanToken();

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(TEST_PASSWORD);
        dto.setNewPassword(TEST_PASSWORD);
        dto.setConfirmPassword(TEST_PASSWORD);

        String response = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10005, extractCode(response));
        assertTrue(extractMessage(response).contains("新密码不能与旧密码相同"));
    }

    @Test
    @DisplayName("修改密码失败 - 确认密码与新密码不一致")
    void testChangePassword_fail_confirmPasswordMismatch() throws Exception {
        String token = getZhangsanToken();

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(TEST_PASSWORD);
        dto.setNewPassword("NewPass@123");
        dto.setConfirmPassword("Different@123");

        String response = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10006, extractCode(response));
        assertTrue(extractMessage(response).contains("确认密码与新密码不一致"));
    }

    @Test
    @DisplayName("修改密码失败 - 弱密码检测 password")
    void testChangePassword_fail_weakPassword_password() throws Exception {
        String token = getZhangsanToken();

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(TEST_PASSWORD);
        dto.setNewPassword("password");
        dto.setConfirmPassword("password");

        String response = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10007, extractCode(response));
        assertTrue(extractMessage(response).contains("密码过于常见"));
    }

    @Test
    @DisplayName("修改密码失败 - 弱密码检测 123456")
    void testChangePassword_fail_weakPassword_123456() throws Exception {
        String token = getZhangsanToken();

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(TEST_PASSWORD);
        dto.setNewPassword("12345678");
        dto.setConfirmPassword("12345678");

        String response = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10007, extractCode(response));
        assertTrue(extractMessage(response).contains("密码过于常见"));
    }

    @Test
    @DisplayName("修改密码失败 - 弱密码检测 admin")
    void testChangePassword_fail_weakPassword_admin() throws Exception {
        String token = getZhangsanToken();

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(TEST_PASSWORD);
        dto.setNewPassword("admin12345");
        dto.setConfirmPassword("admin12345");

        String response = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10007, extractCode(response));
        assertTrue(extractMessage(response).contains("密码过于常见"));
    }

    @Test
    @DisplayName("修改密码失败 - 弱密码检测 qwerty")
    void testChangePassword_fail_weakPassword_qwerty() throws Exception {
        String token = getZhangsanToken();

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(TEST_PASSWORD);
        dto.setNewPassword("qwerty123");
        dto.setConfirmPassword("qwerty123");

        String response = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10007, extractCode(response));
        assertTrue(extractMessage(response).contains("密码过于常见"));
    }

    @Test
    @DisplayName("修改密码失败 - 密码复杂度不足 - 仅字母")
    void testChangePassword_fail_insufficientComplexity_onlyLetters() throws Exception {
        String token = getZhangsanToken();

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(TEST_PASSWORD);
        dto.setNewPassword("abcdefgh");
        dto.setConfirmPassword("abcdefgh");

        String response = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10008, extractCode(response));
        assertTrue(extractMessage(response).contains("密码复杂度不足"));
    }

    @Test
    @DisplayName("修改密码失败 - 密码复杂度不足 - 仅数字")
    void testChangePassword_fail_insufficientComplexity_onlyNumbers() throws Exception {
        String token = getZhangsanToken();

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(TEST_PASSWORD);
        dto.setNewPassword("13579246");
        dto.setConfirmPassword("13579246");

        String response = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10008, extractCode(response));
        assertTrue(extractMessage(response).contains("密码复杂度不足"));
    }

    @Test
    @DisplayName("修改密码失败 - 缺少必填字段 - oldPassword为空")
    void testChangePassword_fail_missingOldPassword() throws Exception {
        String token = getZhangsanToken();

        Map<String, String> body = new HashMap<>();
        body.put("newPassword", "NewPass@123");
        body.put("confirmPassword", "NewPass@123");

        String response = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertNotEquals(200, extractCode(response));
    }

    @Test
    @DisplayName("修改密码失败 - 缺少必填字段 - newPassword为空")
    void testChangePassword_fail_missingNewPassword() throws Exception {
        String token = getZhangsanToken();

        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", TEST_PASSWORD);
        body.put("confirmPassword", "NewPass@123");

        String response = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertNotEquals(200, extractCode(response));
    }

    @Test
    @DisplayName("修改密码失败 - 缺少必填字段 - confirmPassword为空")
    void testChangePassword_fail_missingConfirmPassword() throws Exception {
        String token = getZhangsanToken();

        Map<String, String> body = new HashMap<>();
        body.put("oldPassword", TEST_PASSWORD);
        body.put("newPassword", "NewPass@123");

        String response = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertNotEquals(200, extractCode(response));
    }

    @Test
    @DisplayName("修改密码后旧access token失效 - 返回401")
    void testChangePassword_oldAccessTokenInvalidated() throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "zhangsan");
        loginData.put("password", TEST_PASSWORD);

        String loginResponse = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginData)))
                .andReturn().getResponse().getContentAsString();

        String oldAccessToken = extractData(loginResponse).path("token").asText();
        String oldRefreshToken = extractData(loginResponse).path("refreshToken").asText();

        mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(oldAccessToken))
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());

        Thread.sleep(100);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(TEST_PASSWORD);
        dto.setNewPassword("TempPass@123");
        dto.setConfirmPassword("TempPass@123");

        String changeResponse = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(oldAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(changeResponse));

        String invalidAccessResponse = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(oldAccessToken))
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertTrue(invalidAccessResponse.contains("密码已修改"));

        Map<String, String> refreshBody = new HashMap<>();
        refreshBody.put("refreshToken", oldRefreshToken);

        String invalidRefreshResponse = mockMvc.perform(post("/api/user/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10015, extractCode(invalidRefreshResponse));

        String newToken = loginAndGetToken("zhangsan", "TempPass@123");
        ChangePasswordDTO resetDto = new ChangePasswordDTO();
        resetDto.setOldPassword("TempPass@123");
        resetDto.setNewPassword(TEST_PASSWORD);
        resetDto.setConfirmPassword(TEST_PASSWORD);

        mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(newToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("修改密码后旧refresh token失效 - 返回错误码10015")
    void testChangePassword_oldRefreshTokenInvalidated() throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "lisi");
        loginData.put("password", TEST_PASSWORD);

        String loginResponse = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginData)))
                .andReturn().getResponse().getContentAsString();

        String oldAccessToken = extractData(loginResponse).path("token").asText();
        String oldRefreshToken = extractData(loginResponse).path("refreshToken").asText();

        Thread.sleep(100);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword(TEST_PASSWORD);
        dto.setNewPassword("LisiPass@123");
        dto.setConfirmPassword("LisiPass@123");

        String changeResponse = mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(oldAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(changeResponse));

        Map<String, String> refreshBody = new HashMap<>();
        refreshBody.put("refreshToken", oldRefreshToken);

        String refreshResponse = mockMvc.perform(post("/api/user/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(10015, extractCode(refreshResponse));
        assertTrue(extractMessage(refreshResponse).contains("密码已修改"));

        String newToken = loginAndGetToken("lisi", "LisiPass@123");
        ChangePasswordDTO resetDto = new ChangePasswordDTO();
        resetDto.setOldPassword("LisiPass@123");
        resetDto.setNewPassword(TEST_PASSWORD);
        resetDto.setConfirmPassword(TEST_PASSWORD);

        mockMvc.perform(put("/api/user/change-password")
                        .header("Authorization", buildAuthHeader(newToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetDto)))
                .andExpect(status().isOk());
    }
}
