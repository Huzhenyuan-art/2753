package com.example.usermanager.base;

import com.example.usermanager.config.TestConfig;
import com.example.usermanager.util.JwtUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtUtils jwtUtils;

    protected static final String TEST_PASSWORD = "123456";
    protected static final String STRONG_PASSWORD = "Strong@12345";

    protected String loginAndGetToken(String username, String password) throws Exception {
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", username);
        loginData.put("password", password);

        MvcResult result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginData)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.path("data").path("token").asText();
    }

    protected String buildAuthHeader(String token) {
        return "Bearer " + token;
    }

    protected String getAdminToken() throws Exception {
        return loginAndGetToken("admin", TEST_PASSWORD);
    }

    protected String getZhangsanToken() throws Exception {
        return loginAndGetToken("zhangsan", TEST_PASSWORD);
    }

    protected String getLisiToken() throws Exception {
        return loginAndGetToken("lisi", TEST_PASSWORD);
    }

    protected Map<String, Object> parseResult(String json) throws Exception {
        Map<String, Object> map = objectMapper.readValue(json, HashMap.class);
        return map;
    }

    protected int extractCode(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.path("code").asInt();
    }

    protected String extractMessage(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.path("message").asText();
    }

    protected JsonNode extractData(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.path("data");
    }

    protected String generateExpiredAccessToken(String username, Long userId) {
        String token = jwtUtils.generateAccessToken(username, userId, List.of("ADMIN"), List.of("user:list"));
        return token;
    }
}
