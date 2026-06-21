package com.example.usermanager.controller;

import com.example.usermanager.base.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HealthCheckIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("健康检查基本接口 - /actuator/health 返回UP")
    void testActuatorHealth_success() throws Exception {
        String response = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        assertEquals("UP", jsonNode.path("status").asText());
        assertTrue(jsonNode.path("timestamp").asLong() > 0);
    }

    @Test
    @DisplayName("健康检查接口 - /api/health 返回code=200")
    void testApiHealth_success() throws Exception {
        String response = mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);
        assertEquals("UP", data.path("status").asText());
        assertTrue(data.path("timestamp").asLong() > 0);
    }

    @Test
    @DisplayName("详细健康检查 - /api/health/detail 正常路径")
    void testHealthDetail_success() throws Exception {
        String token = getAdminToken();

        String response = mockMvc.perform(get("/api/health/detail")
                        .header("Authorization", buildAuthHeader(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(200, extractCode(response));
        JsonNode data = extractData(response);

        assertEquals("UP", data.path("status").asText());
        assertNotNull(data.path("applicationName").asText());
        assertFalse(data.path("applicationName").asText().isEmpty());
        assertNotNull(data.path("version").asText());
        assertFalse(data.path("version").asText().isEmpty());
        assertNotNull(data.path("startTime").asText());
        assertFalse(data.path("startTime").asText().isEmpty());
        assertTrue(data.path("uptimeSeconds").asLong() >= 0);

        JsonNode database = data.path("database");
        assertNotNull(database);
        assertFalse(database.isNull());
        assertEquals("UP", database.path("status").asText());
        assertNotNull(database.path("type").asText());
        assertFalse(database.path("type").asText().isEmpty());
        assertTrue(database.path("responseTimeMs").asLong() >= 0);

        JsonNode memory = data.path("memory");
        assertNotNull(memory);
        assertFalse(memory.isNull());
        assertTrue(memory.path("totalMemoryMB").asLong() > 0);
        assertTrue(memory.path("usedMemoryMB").asLong() >= 0);
        assertTrue(memory.path("freeMemoryMB").asLong() >= 0);
        assertTrue(memory.path("usagePercent").asDouble() >= 0);
        assertTrue(memory.path("usagePercent").asDouble() <= 100);

        JsonNode onlineUsers = data.path("onlineUsers");
        assertNotNull(onlineUsers);
        assertFalse(onlineUsers.isNull());
        assertTrue(onlineUsers.path("count").asInt() >= 0);
        assertTrue(onlineUsers.path("totalUsers").asInt() >= 0);
        assertTrue(onlineUsers.path("activeUsersToday").asInt() >= 0);
    }

    @Test
    @DisplayName("详细健康检查 - 未携带token被拒绝")
    void testHealthDetail_noToken_denied() throws Exception {
        mockMvc.perform(get("/api/health/detail"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("详细健康检查 - 携带无效token被拒绝")
    void testHealthDetail_invalidToken_denied() throws Exception {
        mockMvc.perform(get("/api/health/detail")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }
}
