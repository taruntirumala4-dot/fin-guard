package com.finguard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finguard.dto.LoginRequest;
import com.finguard.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class FraudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest request = new LoginRequest("admin@finguard.io", "Password@123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.email").value("admin@finguard.io"))
            .andReturn();

        LoginResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), LoginResponse.class);
        adminToken = response.getToken();
    }

    @Test
    @DisplayName("Should fetch all fraud alerts paginated")
    void testGetAlerts() throws Exception {
        mockMvc.perform(get("/api/fraud/alerts")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Should fetch recent alerts without path collision with /alerts/{id}")
    void testGetRecentAlertsRouteCollision() throws Exception {
        mockMvc.perform(get("/api/fraud/alerts/recent")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should fetch fraud statistics")
    void testGetStatistics() throws Exception {
        mockMvc.perform(get("/api/fraud/statistics")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").exists());
    }
}
