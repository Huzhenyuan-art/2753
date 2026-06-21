package com.example.usermanager.controller;

import com.example.usermanager.base.AbstractIntegrationTest;
import com.example.usermanager.dto.UpdateProfileDTO;
import com.example.usermanager.entity.Dept;
import com.example.usermanager.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserCrudIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("查询用户列表 - 正常分页查询")
    void testList_success_pagination() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertNotNull(data);
        assertTrue(data.path("records").isArray());
        assertTrue(data.path("total").asLong() >= 3);
        assertEquals(1, data.path("current").asInt());
        assertEquals(10, data.path("size").asInt());
    }

    @Test
    @DisplayName("查询用户列表 - 按用户名搜索")
    void testList_success_searchByUsername() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertTrue(data.path("total").asLong() >= 1);
        JsonNode records = data.path("records");
        boolean foundAdmin = false;
        for (JsonNode record : records) {
            if ("admin".equals(record.path("username").asText())) {
                foundAdmin = true;
                break;
            }
        }
        assertTrue(foundAdmin);
    }

    @Test
    @DisplayName("查询用户列表 - 按昵称搜索")
    void testList_success_searchByNickname() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("username", "张三"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertTrue(data.path("total").asLong() >= 1);
    }

    @Test
    @DisplayName("查询用户列表 - 按状态过滤")
    void testList_success_filterByStatus() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        JsonNode records = data.path("records");
        for (JsonNode record : records) {
            assertEquals(1, record.path("status").asInt());
        }
    }

    @Test
    @DisplayName("查询用户列表 - 按用户名字段升序排序")
    void testList_success_sortByUsernameAsc() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "100")
                        .param("sortField", "username")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        JsonNode records = data.path("records");
        String prev = "";
        for (JsonNode record : records) {
            String current = record.path("username").asText();
            assertTrue(current.compareTo(prev) >= 0);
            prev = current;
        }
    }

    @Test
    @DisplayName("查询用户列表 - 按用户名字段降序排序")
    void testList_success_sortByUsernameDesc() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "100")
                        .param("sortField", "username")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        JsonNode records = data.path("records");
        String prev = "zzzzzzzz";
        for (JsonNode record : records) {
            String current = record.path("username").asText();
            assertTrue(current.compareTo(prev) <= 0);
            prev = current;
        }
    }

    @Test
    @DisplayName("查询用户列表 - 按创建时间升序排序")
    void testList_success_sortByCreateTimeAsc() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "100")
                        .param("sortField", "createTime")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        assertNotNull(extractData(response));
    }

    @Test
    @DisplayName("新增用户 - 成功（包含部门）")
    void testAdd_success_withDept() throws Exception {
        String token = getAdminToken();

        User user = new User();
        user.setUsername("testuser_crud_" + System.currentTimeMillis());
        user.setPassword(STRONG_PASSWORD);
        user.setNickname("测试用户CRUD");
        user.setEmail("testcrud@example.com");
        user.setStatus(1);
        List<Long> deptIds = new ArrayList<>();
        deptIds.add(1L);
        user.setDeptIds(deptIds);

        String response = mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertNotNull(data);
        assertTrue(data.asLong() > 0);
    }

    @Test
    @DisplayName("新增用户 - 失败（用户名已存在）")
    void testAdd_fail_duplicateUsername() throws Exception {
        String token = getAdminToken();

        User user = new User();
        user.setUsername("admin");
        user.setPassword(STRONG_PASSWORD);
        user.setNickname("重复用户");
        user.setEmail("dup@example.com");
        user.setStatus(1);

        String response = mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(400, extractCode(response));
        assertTrue(extractMessage(response).contains("已存在"));
    }

    @Test
    @DisplayName("新增用户 - 失败（用户名为空）")
    void testAdd_fail_emptyUsername() throws Exception {
        String token = getAdminToken();

        User user = new User();
        user.setUsername("");
        user.setPassword(STRONG_PASSWORD);
        user.setNickname("测试用户");
        user.setEmail("test@example.com");
        user.setStatus(1);

        mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("新增用户 - 失败（密码为空）")
    void testAdd_fail_emptyPassword() throws Exception {
        String token = getAdminToken();

        User user = new User();
        user.setUsername("test_empty_pwd_" + System.currentTimeMillis());
        user.setPassword("");
        user.setNickname("测试用户");
        user.setEmail("test@example.com");
        user.setStatus(1);

        mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("新增用户 - 失败（密码长度不足6位）")
    void testAdd_fail_shortPassword() throws Exception {
        String token = getAdminToken();

        User user = new User();
        user.setUsername("test_short_pwd_" + System.currentTimeMillis());
        user.setPassword("123");
        user.setNickname("测试用户");
        user.setEmail("test@example.com");
        user.setStatus(1);

        mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("新增用户 - 失败（昵称为空）")
    void testAdd_fail_emptyNickname() throws Exception {
        String token = getAdminToken();

        User user = new User();
        user.setUsername("test_empty_nick_" + System.currentTimeMillis());
        user.setPassword(STRONG_PASSWORD);
        user.setNickname("");
        user.setEmail("test@example.com");
        user.setStatus(1);

        mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("新增用户 - 失败（邮箱格式不正确）")
    void testAdd_fail_invalidEmail() throws Exception {
        String token = getAdminToken();

        User user = new User();
        user.setUsername("test_invalid_email_" + System.currentTimeMillis());
        user.setPassword(STRONG_PASSWORD);
        user.setNickname("测试用户");
        user.setEmail("invalid-email");
        user.setStatus(1);

        mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("编辑用户 - 成功")
    void testUpdate_success() throws Exception {
        String token = getAdminToken();

        String username = "test_update_" + System.currentTimeMillis();
        User user = new User();
        user.setUsername(username);
        user.setPassword(STRONG_PASSWORD);
        user.setNickname("编辑前昵称");
        user.setEmail("before@example.com");
        user.setStatus(1);

        String addResponse = mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andReturn().getResponse().getContentAsString();

        Long userId = extractData(addResponse).asLong();

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setUsername(username);
        updateUser.setNickname("编辑后昵称");
        updateUser.setEmail("after@example.com");
        updateUser.setStatus(1);

        String updateResponse = mockMvc.perform(put("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(updateResponse));
    }

    @Test
    @DisplayName("编辑用户 - 失败（用户名冲突）")
    void testUpdate_fail_usernameConflict() throws Exception {
        String token = getAdminToken();

        String username = "test_conflict_" + System.currentTimeMillis();
        User user = new User();
        user.setUsername(username);
        user.setPassword(STRONG_PASSWORD);
        user.setNickname("测试冲突");
        user.setEmail("conflict@example.com");
        user.setStatus(1);

        String addResponse = mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andReturn().getResponse().getContentAsString();

        Long userId = extractData(addResponse).asLong();

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setUsername("admin");
        updateUser.setNickname("冲突测试");
        updateUser.setStatus(1);

        String updateResponse = mockMvc.perform(put("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(400, extractCode(updateResponse));
        assertTrue(extractMessage(updateResponse).contains("已被占用"));
    }

    @Test
    @DisplayName("删除用户 - 成功")
    void testDelete_success() throws Exception {
        String token = getAdminToken();

        String username = "test_delete_" + System.currentTimeMillis();
        User user = new User();
        user.setUsername(username);
        user.setPassword(STRONG_PASSWORD);
        user.setNickname("待删除用户");
        user.setEmail("delete@example.com");
        user.setStatus(1);

        String addResponse = mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andReturn().getResponse().getContentAsString();

        Long userId = extractData(addResponse).asLong();

        String deleteResponse = mockMvc.perform(delete("/api/user/" + userId)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(deleteResponse));
    }

    @Test
    @DisplayName("删除用户 - 失败（不能删除admin）")
    void testDelete_fail_admin() throws Exception {
        String token = getAdminToken();

        String listResponse = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "100")
                        .param("username", "admin"))
                .andReturn().getResponse().getContentAsString();

        JsonNode records = extractData(listResponse).path("records");
        Long adminId = null;
        for (JsonNode record : records) {
            if ("admin".equals(record.path("username").asText())) {
                adminId = record.path("id").asLong();
                break;
            }
        }
        assertNotNull(adminId);

        String deleteResponse = mockMvc.perform(delete("/api/user/" + adminId)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(403, extractCode(deleteResponse));
        assertTrue(extractMessage(deleteResponse).contains("不允许删除"));
    }

    @Test
    @DisplayName("切换用户状态 - 禁用用户成功")
    void testUpdateStatus_success_disable() throws Exception {
        String token = getAdminToken();

        String username = "test_status_" + System.currentTimeMillis();
        User user = new User();
        user.setUsername(username);
        user.setPassword(STRONG_PASSWORD);
        user.setNickname("状态测试用户");
        user.setEmail("status@example.com");
        user.setStatus(1);

        String addResponse = mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andReturn().getResponse().getContentAsString();

        Long userId = extractData(addResponse).asLong();

        String statusResponse = mockMvc.perform(put("/api/user/" + userId + "/status")
                        .header("Authorization", buildAuthHeader(token))
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(statusResponse));
        assertTrue(extractMessage(statusResponse).contains("已禁用"));
    }

    @Test
    @DisplayName("切换用户状态 - 启用用户成功")
    void testUpdateStatus_success_enable() throws Exception {
        String token = getAdminToken();

        String username = "test_enable_" + System.currentTimeMillis();
        User user = new User();
        user.setUsername(username);
        user.setPassword(STRONG_PASSWORD);
        user.setNickname("启用测试用户");
        user.setEmail("enable@example.com");
        user.setStatus(0);

        String addResponse = mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andReturn().getResponse().getContentAsString();

        Long userId = extractData(addResponse).asLong();

        String statusResponse = mockMvc.perform(put("/api/user/" + userId + "/status")
                        .header("Authorization", buildAuthHeader(token))
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(statusResponse));
        assertTrue(extractMessage(statusResponse).contains("已启用"));
    }

    @Test
    @DisplayName("切换用户状态 - 失败（不能禁用admin）")
    void testUpdateStatus_fail_admin() throws Exception {
        String token = getAdminToken();

        String listResponse = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "100")
                        .param("username", "admin"))
                .andReturn().getResponse().getContentAsString();

        JsonNode records = extractData(listResponse).path("records");
        Long adminId = null;
        for (JsonNode record : records) {
            if ("admin".equals(record.path("username").asText())) {
                adminId = record.path("id").asLong();
                break;
            }
        }
        assertNotNull(adminId);

        String statusResponse = mockMvc.perform(put("/api/user/" + adminId + "/status")
                        .header("Authorization", buildAuthHeader(token))
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(403, extractCode(statusResponse));
        assertTrue(extractMessage(statusResponse).contains("不允许禁用"));
    }

    @Test
    @DisplayName("切换用户状态 - 失败（用户不存在）")
    void testUpdateStatus_fail_userNotFound() throws Exception {
        String token = getAdminToken();

        String statusResponse = mockMvc.perform(put("/api/user/999999/status")
                        .header("Authorization", buildAuthHeader(token))
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(404, extractCode(statusResponse));
        assertTrue(extractMessage(statusResponse).contains("用户不存在"));
    }

    @Test
    @DisplayName("切换用户状态 - 失败（非法状态值-负数）")
    void testUpdateStatus_fail_invalidStatusNegative() throws Exception {
        String token = getAdminToken();

        String username = "test_invalid_status_" + System.currentTimeMillis();
        User user = new User();
        user.setUsername(username);
        user.setPassword(STRONG_PASSWORD);
        user.setNickname("非法状态测试");
        user.setEmail("invalid@example.com");
        user.setStatus(1);

        String addResponse = mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andReturn().getResponse().getContentAsString();

        Long userId = extractData(addResponse).asLong();

        String statusResponse = mockMvc.perform(put("/api/user/" + userId + "/status")
                        .header("Authorization", buildAuthHeader(token))
                        .param("status", "-1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(400, extractCode(statusResponse));
        assertTrue(extractMessage(statusResponse).contains("不合法"));
    }

    @Test
    @DisplayName("切换用户状态 - 失败（非法状态值-大于1）")
    void testUpdateStatus_fail_invalidStatusGreater() throws Exception {
        String token = getAdminToken();

        String username = "test_invalid_status2_" + System.currentTimeMillis();
        User user = new User();
        user.setUsername(username);
        user.setPassword(STRONG_PASSWORD);
        user.setNickname("非法状态测试2");
        user.setEmail("invalid2@example.com");
        user.setStatus(1);

        String addResponse = mockMvc.perform(post("/api/user")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andReturn().getResponse().getContentAsString();

        Long userId = extractData(addResponse).asLong();

        String statusResponse = mockMvc.perform(put("/api/user/" + userId + "/status")
                        .header("Authorization", buildAuthHeader(token))
                        .param("status", "2"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(400, extractCode(statusResponse));
        assertTrue(extractMessage(statusResponse).contains("不合法"));
    }

    @Test
    @DisplayName("查询用户名是否可用 - 新用户名可用")
    void testCheckUsername_available() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/user/check-username")
                        .header("Authorization", buildAuthHeader(token))
                        .param("username", "new_unique_user_" + System.currentTimeMillis()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        assertTrue(extractData(response).asBoolean());
    }

    @Test
    @DisplayName("查询用户名是否可用 - 用户名已存在")
    void testCheckUsername_notAvailable() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/user/check-username")
                        .header("Authorization", buildAuthHeader(token))
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        assertFalse(extractData(response).asBoolean());
    }

    @Test
    @DisplayName("查询用户名是否可用 - 排除自身ID时可用")
    void testCheckUsername_withExcludeId() throws Exception {
        String token = getAdminToken();

        String listResponse = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "100")
                        .param("username", "zhangsan"))
                .andReturn().getResponse().getContentAsString();

        JsonNode records = extractData(listResponse).path("records");
        Long zhangsanId = null;
        for (JsonNode record : records) {
            if ("zhangsan".equals(record.path("username").asText())) {
                zhangsanId = record.path("id").asLong();
                break;
            }
        }
        assertNotNull(zhangsanId);

        String response = mockMvc.perform(get("/api/user/check-username")
                        .header("Authorization", buildAuthHeader(token))
                        .param("username", "zhangsan")
                        .param("excludeId", String.valueOf(zhangsanId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        assertTrue(extractData(response).asBoolean());
    }

    @Test
    @DisplayName("更新个人资料 - 成功")
    void testUpdateProfile_success() throws Exception {
        String token = getZhangsanToken();

        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNickname("张三新昵称");
        dto.setEmail("zhangsan_new@example.com");

        String response = mockMvc.perform(put("/api/user/profile")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        assertTrue(extractMessage(response).contains("更新成功"));

        UpdateProfileDTO revertDto = new UpdateProfileDTO();
        revertDto.setNickname("张三");
        revertDto.setEmail("zhangsan@example.com");

        mockMvc.perform(put("/api/user/profile")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revertDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("更新个人资料 - 失败（昵称为空）")
    void testUpdateProfile_fail_emptyNickname() throws Exception {
        String token = getZhangsanToken();

        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNickname("");
        dto.setEmail("zhangsan@example.com");

        mockMvc.perform(put("/api/user/profile")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("更新个人资料 - 失败（邮箱格式不正确）")
    void testUpdateProfile_fail_invalidEmail() throws Exception {
        String token = getZhangsanToken();

        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNickname("张三");
        dto.setEmail("invalid-email-format");

        mockMvc.perform(put("/api/user/profile")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
