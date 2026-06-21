package com.example.usermanager.controller;

import com.example.usermanager.base.AbstractIntegrationTest;
import com.example.usermanager.entity.Permission;
import com.example.usermanager.entity.Role;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RolePermissionIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("角色列表查询 - 正常路径")
    void testRoleList_success() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/role/list")
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertNotNull(data);
        assertTrue(data.isArray());
        assertTrue(data.size() >= 1);
        assertTrue(data.toString().contains("ADMIN"));
    }

    @Test
    @DisplayName("根据ID查询角色 - 正常路径")
    void testGetRoleById_success() throws Exception {
        String token = getAdminToken();

        String listResponse = mockMvc.perform(get("/api/role/list")
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode roleList = extractData(listResponse);
        Long roleId = roleList.get(0).path("id").asLong();

        String response = mockMvc.perform(get("/api/role/" + roleId)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertNotNull(data);
        assertEquals(roleId, data.path("id").asLong());
        assertNotNull(data.path("name").asText());
        assertNotNull(data.path("code").asText());
    }

    @Test
    @DisplayName("根据ID查询角色 - 角色不存在")
    void testGetRoleById_notFound() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/role/99999")
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(404, extractCode(response));
        assertTrue(extractMessage(response).contains("角色不存在"));
    }

    @Test
    @DisplayName("新增角色 - 成功")
    void testAddRole_success() throws Exception {
        String token = getAdminToken();

        Role role = new Role();
        role.setName("测试角色");
        role.setCode("TEST_ROLE_" + System.currentTimeMillis());
        role.setDescription("这是一个测试角色");
        role.setStatus(1);

        String response = mockMvc.perform(post("/api/role")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
    }

    @Test
    @DisplayName("新增角色 - 编码重复失败")
    void testAddRole_duplicateCode_fail() throws Exception {
        String token = getAdminToken();
        String code = "DUP_ROLE_" + System.currentTimeMillis();

        Role role1 = new Role();
        role1.setName("重复角色1");
        role1.setCode(code);
        role1.setStatus(1);

        mockMvc.perform(post("/api/role")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role1)))
                .andExpect(status().isOk());

        Role role2 = new Role();
        role2.setName("重复角色2");
        role2.setCode(code);
        role2.setStatus(1);

        String response = mockMvc.perform(post("/api/role")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role2)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(400, extractCode(response));
        assertTrue(extractMessage(response).contains("角色编码已存在"));
    }

    @Test
    @DisplayName("编辑角色 - 成功")
    void testUpdateRole_success() throws Exception {
        String token = getAdminToken();
        String code = "UPDATE_ROLE_" + System.currentTimeMillis();

        Role role = new Role();
        role.setName("待编辑角色");
        role.setCode(code);
        role.setStatus(1);

        String addResponse = mockMvc.perform(post("/api/role")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andReturn().getResponse().getContentAsString();
        assertEquals(200, extractCode(addResponse));

        String listResponse = mockMvc.perform(get("/api/role/list")
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode roleList = extractData(listResponse);
        Long roleId = null;
        for (JsonNode node : roleList) {
            if (code.equals(node.path("code").asText())) {
                roleId = node.path("id").asLong();
                break;
            }
        }
        assertNotNull(roleId);

        Role updateRole = new Role();
        updateRole.setId(roleId);
        updateRole.setName("已编辑角色");
        updateRole.setCode(code);
        updateRole.setDescription("编辑后的描述");
        updateRole.setStatus(1);

        String response = mockMvc.perform(put("/api/role")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRole)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));

        String getResponse = mockMvc.perform(get("/api/role/" + roleId)
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = extractData(getResponse);
        assertEquals("已编辑角色", data.path("name").asText());
        assertEquals("编辑后的描述", data.path("description").asText());
    }

    @Test
    @DisplayName("编辑角色 - 编码冲突失败")
    void testUpdateRole_codeConflict_fail() throws Exception {
        String token = getAdminToken();
        String code1 = "CONFLICT_ROLE1_" + System.currentTimeMillis();
        String code2 = "CONFLICT_ROLE2_" + System.currentTimeMillis();

        Role role1 = new Role();
        role1.setName("冲突角色1");
        role1.setCode(code1);
        role1.setStatus(1);
        mockMvc.perform(post("/api/role")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role1)))
                .andExpect(status().isOk());

        Role role2 = new Role();
        role2.setName("冲突角色2");
        role2.setCode(code2);
        role2.setStatus(1);
        mockMvc.perform(post("/api/role")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role2)))
                .andExpect(status().isOk());

        String listResponse = mockMvc.perform(get("/api/role/list")
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode roleList = extractData(listResponse);
        Long role2Id = null;
        for (JsonNode node : roleList) {
            if (code2.equals(node.path("code").asText())) {
                role2Id = node.path("id").asLong();
                break;
            }
        }
        assertNotNull(role2Id);

        Role updateRole = new Role();
        updateRole.setId(role2Id);
        updateRole.setName("冲突角色2修改");
        updateRole.setCode(code1);
        updateRole.setStatus(1);

        String response = mockMvc.perform(put("/api/role")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRole)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(400, extractCode(response));
        assertTrue(extractMessage(response).contains("角色编码已被占用"));
    }

    @Test
    @DisplayName("删除角色 - 正常路径")
    void testDeleteRole_success() throws Exception {
        String token = getAdminToken();
        String code = "DELETE_ROLE_" + System.currentTimeMillis();

        Role role = new Role();
        role.setName("待删除角色");
        role.setCode(code);
        role.setStatus(1);

        mockMvc.perform(post("/api/role")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk());

        String listResponse = mockMvc.perform(get("/api/role/list")
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode roleList = extractData(listResponse);
        Long roleId = null;
        for (JsonNode node : roleList) {
            if (code.equals(node.path("code").asText())) {
                roleId = node.path("id").asLong();
                break;
            }
        }
        assertNotNull(roleId);

        String response = mockMvc.perform(delete("/api/role/" + roleId)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
    }

    @Test
    @DisplayName("新增角色 - 缺少必填字段失败")
    void testAddRole_missingRequiredFields_fail() throws Exception {
        String token = getAdminToken();

        Role role = new Role();
        role.setDescription("缺少名称和编码");

        mockMvc.perform(post("/api/role")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("新增角色 - 编码为空失败")
    void testAddRole_emptyCode_fail() throws Exception {
        String token = getAdminToken();

        Role role = new Role();
        role.setName("空编码角色");
        role.setCode("");
        role.setStatus(1);

        mockMvc.perform(post("/api/role")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("新增角色 - 名称为空失败")
    void testAddRole_emptyName_fail() throws Exception {
        String token = getAdminToken();

        Role role = new Role();
        role.setName("");
        role.setCode("EMPTY_NAME_ROLE_" + System.currentTimeMillis());
        role.setStatus(1);

        mockMvc.perform(post("/api/role")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("编辑角色 - 缺少必填字段失败")
    void testUpdateRole_missingRequiredFields_fail() throws Exception {
        String token = getAdminToken();

        Role role = new Role();
        role.setId(1L);
        role.setDescription("缺少名称和编码");

        mockMvc.perform(put("/api/role")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("权限列表查询 - 正常路径")
    void testPermissionList_success() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/permission/list")
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertNotNull(data);
        assertTrue(data.isArray());
        assertTrue(data.size() >= 1);
    }

    @Test
    @DisplayName("根据ID查询权限 - 正常路径")
    void testGetPermissionById_success() throws Exception {
        String token = getAdminToken();

        String listResponse = mockMvc.perform(get("/api/permission/list")
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode permissionList = extractData(listResponse);
        assertTrue(permissionList.size() >= 1);
        Long permissionId = permissionList.get(0).path("id").asLong();

        String response = mockMvc.perform(get("/api/permission/" + permissionId)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertNotNull(data);
        assertEquals(permissionId, data.path("id").asLong());
        assertNotNull(data.path("name").asText());
        assertNotNull(data.path("code").asText());
    }

    @Test
    @DisplayName("根据ID查询权限 - 权限不存在")
    void testGetPermissionById_notFound() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/permission/99999")
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(404, extractCode(response));
        assertTrue(extractMessage(response).contains("权限不存在"));
    }

    @Test
    @DisplayName("新增权限 - 成功")
    void testAddPermission_success() throws Exception {
        String token = getAdminToken();

        Permission permission = new Permission();
        permission.setName("测试权限");
        permission.setCode("test:perm:" + System.currentTimeMillis());
        permission.setType("BUTTON");
        permission.setDescription("这是一个测试权限");
        permission.setStatus(1);

        String response = mockMvc.perform(post("/api/permission")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permission)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
    }

    @Test
    @DisplayName("编辑权限 - 成功")
    void testUpdatePermission_success() throws Exception {
        String token = getAdminToken();
        String code = "update:perm:" + System.currentTimeMillis();

        Permission permission = new Permission();
        permission.setName("待编辑权限");
        permission.setCode(code);
        permission.setType("MENU");
        permission.setStatus(1);

        String addResponse = mockMvc.perform(post("/api/permission")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permission)))
                .andReturn().getResponse().getContentAsString();
        assertEquals(200, extractCode(addResponse));

        String listResponse = mockMvc.perform(get("/api/permission/list")
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode permissionList = extractData(listResponse);
        Long permissionId = null;
        for (JsonNode node : permissionList) {
            if (code.equals(node.path("code").asText())) {
                permissionId = node.path("id").asLong();
                break;
            }
        }
        assertNotNull(permissionId);

        Permission updatePermission = new Permission();
        updatePermission.setId(permissionId);
        updatePermission.setName("已编辑权限");
        updatePermission.setCode(code);
        updatePermission.setType("BUTTON");
        updatePermission.setDescription("编辑后的描述");
        updatePermission.setStatus(1);

        String response = mockMvc.perform(put("/api/permission")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePermission)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));

        String getResponse = mockMvc.perform(get("/api/permission/" + permissionId)
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = extractData(getResponse);
        assertEquals("已编辑权限", data.path("name").asText());
        assertEquals("BUTTON", data.path("type").asText());
        assertEquals("编辑后的描述", data.path("description").asText());
    }

    @Test
    @DisplayName("删除权限 - 正常路径")
    void testDeletePermission_success() throws Exception {
        String token = getAdminToken();
        String code = "delete:perm:" + System.currentTimeMillis();

        Permission permission = new Permission();
        permission.setName("待删除权限");
        permission.setCode(code);
        permission.setType("BUTTON");
        permission.setStatus(1);

        mockMvc.perform(post("/api/permission")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permission)))
                .andExpect(status().isOk());

        String listResponse = mockMvc.perform(get("/api/permission/list")
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode permissionList = extractData(listResponse);
        Long permissionId = null;
        for (JsonNode node : permissionList) {
            if (code.equals(node.path("code").asText())) {
                permissionId = node.path("id").asLong();
                break;
            }
        }
        assertNotNull(permissionId);

        String response = mockMvc.perform(delete("/api/permission/" + permissionId)
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
    }

    @Test
    @DisplayName("新增权限 - 缺少必填字段失败")
    void testAddPermission_missingRequiredFields_fail() throws Exception {
        String token = getAdminToken();

        Permission permission = new Permission();
        permission.setDescription("缺少名称和编码");

        mockMvc.perform(post("/api/permission")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permission)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("新增权限 - 编码为空失败")
    void testAddPermission_emptyCode_fail() throws Exception {
        String token = getAdminToken();

        Permission permission = new Permission();
        permission.setName("空编码权限");
        permission.setCode("");
        permission.setStatus(1);

        mockMvc.perform(post("/api/permission")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permission)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("新增权限 - 名称为空失败")
    void testAddPermission_emptyName_fail() throws Exception {
        String token = getAdminToken();

        Permission permission = new Permission();
        permission.setName("");
        permission.setCode("empty:name:" + System.currentTimeMillis());
        permission.setStatus(1);

        mockMvc.perform(post("/api/permission")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permission)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("编辑权限 - 缺少必填字段失败")
    void testUpdatePermission_missingRequiredFields_fail() throws Exception {
        String token = getAdminToken();

        Permission permission = new Permission();
        permission.setId(1L);
        permission.setDescription("缺少名称和编码");

        mockMvc.perform(put("/api/permission")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permission)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("删除权限 - 权限不存在")
    void testDeletePermission_notFound() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(delete("/api/permission/99999")
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
    }

    @Test
    @DisplayName("查询用户已分配的角色ID列表 - 正常路径")
    void testGetUserRoles_success() throws Exception {
        String token = getAdminToken();

        String userListResponse = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andReturn().getResponse().getContentAsString();
        JsonNode userPage = extractData(userListResponse);
        JsonNode records = userPage.path("records");
        assertTrue(records.size() >= 1);
        Long userId = records.get(0).path("id").asLong();

        String response = mockMvc.perform(get("/api/user/" + userId + "/roles")
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertNotNull(data);
        assertTrue(data.isArray());
    }

    @Test
    @DisplayName("分配用户角色 - 单角色成功")
    void testAssignRoles_singleRole_success() throws Exception {
        String token = getAdminToken();

        String userListResponse = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andReturn().getResponse().getContentAsString();
        JsonNode userPage = extractData(userListResponse);
        JsonNode records = userPage.path("records");
        Long userId = null;
        for (JsonNode node : records) {
            if (!"admin".equals(node.path("username").asText())) {
                userId = node.path("id").asLong();
                break;
            }
        }
        assertNotNull(userId);

        String roleListResponse = mockMvc.perform(get("/api/role/list")
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode roleList = extractData(roleListResponse);
        assertTrue(roleList.size() >= 1);
        Long roleId = roleList.get(0).path("id").asLong();

        Map<String, List<Long>> body = new HashMap<>();
        body.put("roleIds", List.of(roleId));

        String response = mockMvc.perform(put("/api/user/" + userId + "/roles")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        assertTrue(extractMessage(response).contains("角色分配成功"));

        String getRolesResponse = mockMvc.perform(get("/api/user/" + userId + "/roles")
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode assignedRoles = extractData(getRolesResponse);
        assertTrue(assignedRoles.isArray());
        assertTrue(assignedRoles.size() >= 1);
        boolean found = false;
        for (JsonNode node : assignedRoles) {
            if (roleId.equals(node.asLong())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    @DisplayName("分配用户角色 - 多角色成功")
    void testAssignRoles_multipleRoles_success() throws Exception {
        String token = getAdminToken();

        String userListResponse = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andReturn().getResponse().getContentAsString();
        JsonNode userPage = extractData(userListResponse);
        JsonNode records = userPage.path("records");
        Long userId = null;
        for (JsonNode node : records) {
            if (!"admin".equals(node.path("username").asText())) {
                userId = node.path("id").asLong();
                break;
            }
        }
        assertNotNull(userId);

        String roleListResponse = mockMvc.perform(get("/api/role/list")
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode roleList = extractData(roleListResponse);
        assertTrue(roleList.size() >= 2);
        Long roleId1 = roleList.get(0).path("id").asLong();
        Long roleId2 = roleList.get(1).path("id").asLong();

        Map<String, List<Long>> body = new HashMap<>();
        body.put("roleIds", List.of(roleId1, roleId2));

        String response = mockMvc.perform(put("/api/user/" + userId + "/roles")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        assertTrue(extractMessage(response).contains("角色分配成功"));

        String getRolesResponse = mockMvc.perform(get("/api/user/" + userId + "/roles")
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode assignedRoles = extractData(getRolesResponse);
        assertTrue(assignedRoles.isArray());
        assertTrue(assignedRoles.size() >= 2);
    }

    @Test
    @DisplayName("分配用户角色 - 清空角色（空列表）")
    void testAssignRoles_emptyList_clearRoles() throws Exception {
        String token = getAdminToken();

        String userListResponse = mockMvc.perform(get("/api/user/list")
                        .header("Authorization", buildAuthHeader(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andReturn().getResponse().getContentAsString();
        JsonNode userPage = extractData(userListResponse);
        JsonNode records = userPage.path("records");
        Long userId = null;
        for (JsonNode node : records) {
            if (!"admin".equals(node.path("username").asText())) {
                userId = node.path("id").asLong();
                break;
            }
        }
        assertNotNull(userId);

        String roleListResponse = mockMvc.perform(get("/api/role/list")
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode roleList = extractData(roleListResponse);
        assertTrue(roleList.size() >= 1);
        Long roleId = roleList.get(0).path("id").asLong();

        Map<String, List<Long>> assignBody = new HashMap<>();
        assignBody.put("roleIds", List.of(roleId));
        mockMvc.perform(put("/api/user/" + userId + "/roles")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignBody)))
                .andExpect(status().isOk());

        Map<String, List<Long>> clearBody = new HashMap<>();
        clearBody.put("roleIds", List.of());

        String response = mockMvc.perform(put("/api/user/" + userId + "/roles")
                        .header("Authorization", buildAuthHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clearBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        assertTrue(extractMessage(response).contains("角色分配成功"));

        String getRolesResponse = mockMvc.perform(get("/api/user/" + userId + "/roles")
                        .header("Authorization", buildAuthHeader(token)))
                .andReturn().getResponse().getContentAsString();
        JsonNode assignedRoles = extractData(getRolesResponse);
        assertTrue(assignedRoles.isArray());
        assertEquals(0, assignedRoles.size());
    }

    @Test
    @DisplayName("未登录访问角色接口 - 被拒绝")
    void testAccessRoleEndpoint_noToken_denied() throws Exception {
        mockMvc.perform(get("/api/role/list"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录访问权限接口 - 被拒绝")
    void testAccessPermissionEndpoint_noToken_denied() throws Exception {
        mockMvc.perform(get("/api/permission/list"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录访问用户角色接口 - 被拒绝")
    void testAccessUserRolesEndpoint_noToken_denied() throws Exception {
        mockMvc.perform(get("/api/user/1/roles"))
                .andExpect(status().isForbidden());
    }
}
