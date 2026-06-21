package com.example.usermanager.controller;

import com.example.usermanager.base.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AvatarUploadIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("上传头像成功 - JPEG图片")
    void testUploadAvatar_success_jpeg() throws Exception {
        String token = getZhangsanToken();

        byte[] jpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
                (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x08
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                jpegBytes
        );

        String response = mockMvc.perform(multipart("/api/file/avatar")
                        .file(file)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        assertTrue(extractMessage(response).contains("成功"));
        assertNotNull(extractData(response));
        assertTrue(extractData(response).asText().contains("/uploads/avatars/"));
    }

    @Test
    @DisplayName("上传头像成功 - PNG图片")
    void testUploadAvatar_success_png() throws Exception {
        String token = getAdminToken();

        byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                pngBytes
        );

        String response = mockMvc.perform(multipart("/api/file/avatar")
                        .file(file)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        assertNotNull(extractData(response));
        assertTrue(extractData(response).asText().contains("/uploads/avatars/"));
    }

    @Test
    @DisplayName("上传头像失败 - 文件为空")
    void testUploadAvatar_fail_emptyFile() throws Exception {
        String token = getZhangsanToken();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "",
                "image/jpeg",
                new byte[0]
        );

        String response = mockMvc.perform(multipart("/api/file/avatar")
                        .file(file)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(400, extractCode(response));
        assertTrue(extractMessage(response).contains("请选择要上传的文件"));
    }

    @Test
    @DisplayName("上传头像失败 - 非图片类型 text/plain")
    void testUploadAvatar_fail_notImage_textPlain() throws Exception {
        String token = getLisiToken();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "This is plain text content".getBytes()
        );

        String response = mockMvc.perform(multipart("/api/file/avatar")
                        .file(file)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(400, extractCode(response));
        assertTrue(extractMessage(response).contains("仅支持上传图片文件"));
    }

    @Test
    @DisplayName("上传头像失败 - 非图片类型 application/pdf")
    void testUploadAvatar_fail_notImage_pdf() throws Exception {
        String token = getAdminToken();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.pdf",
                "application/pdf",
                "%PDF-1.4 test content".getBytes()
        );

        String response = mockMvc.perform(multipart("/api/file/avatar")
                        .file(file)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(400, extractCode(response));
        assertTrue(extractMessage(response).contains("仅支持上传图片文件"));
    }

    @Test
    @DisplayName("上传头像失败 - 超过大小限制 20MB")
    void testUploadAvatar_fail_exceedSizeLimit() throws Exception {
        String token = getZhangsanToken();

        int largeSize = 21 * 1024 * 1024;
        byte[] largeBytes = new byte[largeSize];
        largeBytes[0] = (byte) 0xFF;
        largeBytes[1] = (byte) 0xD8;
        largeBytes[2] = (byte) 0xFF;
        largeBytes[3] = (byte) 0xE0;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large_avatar.jpg",
                "image/jpeg",
                largeBytes
        );

        String response = mockMvc.perform(multipart("/api/file/avatar")
                        .file(file)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(413, extractCode(response));
        assertTrue(extractMessage(response).contains("图片大小不能超过20MB"));
    }

    @Test
    @DisplayName("上传头像失败 - 未携带认证token")
    void testUploadAvatar_fail_noAuthToken() throws Exception {
        byte[] jpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                jpegBytes
        );

        mockMvc.perform(multipart("/api/file/avatar")
                        .file(file))
                .andExpect(status().isForbidden());
    }
}
