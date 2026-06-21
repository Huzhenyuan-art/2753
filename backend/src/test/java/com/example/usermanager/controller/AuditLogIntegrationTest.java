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

class AuditLogIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("查询审计日志列表成功 - 分页空条件")
    void testListAuditLogs_success_pagination() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/audit-log/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertNotNull(data);
        assertNotNull(data.path("records"));
        assertTrue(data.path("records").isArray());
        assertTrue(data.path("current").asLong() >= 1);
        assertTrue(data.path("size").asLong() >= 1);
    }

    @Test
    @DisplayName("查询审计日志列表 - 按用户名筛选")
    void testListAuditLogs_filterByUsername() throws Exception {
        String token = getAdminToken();

        Map<String, Object> newUser = new HashMap<>();
        newUser.put("username", "audit_test_user");
        newUser.put("password", STRONG_PASSWORD);
        newUser.put("nickname", "审计测试用户");
        newUser.put("email", "audit_test@example.com");
        newUser.put("status", 1);

        mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk());

        Thread.sleep(500);

        String response = mockMvc.perform(get("/api/audit-log/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertNotNull(data.path("records"));
        assertTrue(data.path("records").isArray());

        JsonNode records = data.path("records");
        for (JsonNode record : records) {
            assertEquals("admin", record.path("username").asText());
        }
    }

    @Test
    @DisplayName("查询审计日志列表 - 按操作类型筛选")
    void testListAuditLogs_filterByOperation() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/audit-log/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("operation", "LOGIN"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertNotNull(data.path("records"));
        assertTrue(data.path("records").isArray());

        JsonNode records = data.path("records");
        for (JsonNode record : records) {
            assertEquals("LOGIN", record.path("operation").asText());
        }
    }

    @Test
    @DisplayName("根据ID查询审计日志详情 - 成功")
    void testGetAuditLogById_success() throws Exception {
        String token = getAdminToken();

        String listResponse = mockMvc.perform(get("/api/audit-log/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode listData = extractData(listResponse);
        JsonNode records = listData.path("records");
        assertTrue(records.size() > 0, "需要至少一条审计日志记录");

        Long logId = records.get(0).path("id").asLong();

        String detailResponse = mockMvc.perform(get("/api/audit-log/{id}", logId)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(detailResponse));
        JsonNode detailData = extractData(detailResponse);
        assertEquals(logId, detailData.path("id").asLong());
        assertNotNull(detailData.path("operation").asText());
        assertNotNull(detailData.path("username").asText());
    }

    @Test
    @DisplayName("根据ID查询审计日志详情 - 不存在返回404")
    void testGetAuditLogById_notFound() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/audit-log/{id}", 99999999L)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(404, extractCode(response));
        assertTrue(extractMessage(response).contains("日志不存在"));
    }

    @Test
    @DisplayName("新增用户后验证审计日志已记录")
    void testAuditLogRecorded_afterCreateUser() throws Exception {
        String token = getAdminToken();

        Map<String, Object> newUser = new HashMap<>();
        newUser.put("username", "audit_create_user");
        newUser.put("password", STRONG_PASSWORD);
        newUser.put("nickname", "审计创建用户");
        newUser.put("email", "audit_create@example.com");
        newUser.put("status", 1);

        mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk());

        Thread.sleep(500);

        String listResponse = mockMvc.perform(get("/api/audit-log/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("username", "admin")
                        .param("operation", "CREATE"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(listResponse));
        JsonNode data = extractData(listResponse);
        assertNotNull(data.path("records"));
        assertTrue(data.path("records").isArray());
        assertTrue(data.path("total").asLong() > 0, "应存在CREATE类型的审计日志");

        JsonNode records = data.path("records");
        boolean found = false;
        for (JsonNode record : records) {
            if ("CREATE".equals(record.path("operation").asText())
                    && "用户管理".equals(record.path("module").asText())) {
                found = true;
                assertEquals("admin", record.path("username").asText());
                break;
            }
        }
        assertTrue(found, "应找到新增用户的审计日志");
    }

    @Test
    @DisplayName("删除用户后验证审计日志已记录")
    void testAuditLogRecorded_afterDeleteUser() throws Exception {
        String token = getAdminToken();

        Map<String, Object> newUser = new HashMap<>();
        newUser.put("username", "audit_delete_user");
        newUser.put("password", STRONG_PASSWORD);
        newUser.put("nickname", "审计删除用户");
        newUser.put("email", "audit_delete@example.com");
        newUser.put("status", 1);

        String createResponse = mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long userId = extractData(createResponse).asLong();

        Thread.sleep(500);

        mockMvc.perform(delete("/api/user/{id}", userId)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk());

        Thread.sleep(500);

        String listResponse = mockMvc.perform(get("/api/audit-log/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("username", "admin")
                        .param("operation", "DELETE"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(listResponse));
        JsonNode data = extractData(listResponse);
        assertTrue(data.path("total").asLong() > 0, "应存在DELETE类型的审计日志");

        JsonNode records = data.path("records");
        boolean found = false;
        for (JsonNode record : records) {
            if ("DELETE".equals(record.path("operation").asText())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "应找到删除用户的审计日志");
    }

    @Test
    @DisplayName("切换用户状态后验证审计日志已记录")
    void testAuditLogRecorded_afterStatusChange() throws Exception {
        String token = getAdminToken();

        Map<String, Object> newUser = new HashMap<>();
        newUser.put("username", "audit_status_user");
        newUser.put("password", STRONG_PASSWORD);
        newUser.put("nickname", "审计状态用户");
        newUser.put("email", "audit_status@example.com");
        newUser.put("status", 1);

        String createResponse = mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long userId = extractData(createResponse).asLong();

        Thread.sleep(500);

        mockMvc.perform(put("/api/user/{id}/status", userId)
                        .header("Authorization", buildAuthHeader(token))
                        .param("status", "0"))
                .andExpect(status().isOk());

        Thread.sleep(500);

        String listResponse = mockMvc.perform(get("/api/audit-log/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("username", "admin")
                        .param("operation", "STATUS"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(listResponse));
        JsonNode data = extractData(listResponse);
        assertTrue(data.path("total").asLong() > 0, "应存在STATUS类型的审计日志");

        JsonNode records = data.path("records");
        boolean found = false;
        for (JsonNode record : records) {
            if ("STATUS".equals(record.path("operation").asText())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "应找到切换用户状态的审计日志");
    }
}
