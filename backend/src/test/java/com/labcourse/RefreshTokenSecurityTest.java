package com.labcourse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labcourse.entity.Student;
import com.labcourse.entity.Teacher;
import com.labcourse.entity.Admin;
import com.labcourse.repository.StudentRepository;
import com.labcourse.repository.TeacherRepository;
import com.labcourse.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for Refresh Token leakage (MEDIUM-002).
 * Verifies that refreshToken is NOT exposed in JSON serialization of
 * Student/Teacher entities, and that the BFF login response
 * does not leak refreshToken to the frontend.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
class RefreshTokenSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Test
    @Order(1)
    @DisplayName("RefreshToken: Student login response does not expose refreshToken in data object")
    void testStudentLogin_RefreshTokenNotInData() throws Exception {
        String loginBody = objectMapper.writeValueAsString(
                Map.of("studentNo", "S001", "password", "123456"));

        String resp = mockMvc.perform(post("/api/student/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(resp, Map.class);

        // The top-level refreshToken is expected (used by BFF to set HttpOnly cookie)
        assertNotNull(response.get("refreshToken"), "Top-level refreshToken should exist for BFF");

        // The data object (student entity) should NOT contain refreshToken
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertNotNull(data, "data object should exist");
        assertNull(data.get("refreshToken"),
                "refreshToken should NOT be in the serialized student entity (WRITE_ONLY)");
        assertNull(data.get("password"),
                "password should NOT be in the serialized student entity (WRITE_ONLY)");
    }

    @Test
    @Order(2)
    @DisplayName("RefreshToken: Teacher login response does not expose refreshToken in data object")
    void testTeacherLogin_RefreshTokenNotInData() throws Exception {
        String loginBody = objectMapper.writeValueAsString(
                Map.of("teacherNo", "T001", "password", "123456"));

        String resp = mockMvc.perform(post("/api/teacher/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(resp, Map.class);

        assertNotNull(response.get("refreshToken"), "Top-level refreshToken should exist for BFF");

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertNotNull(data, "data object should exist");
        assertNull(data.get("refreshToken"),
                "refreshToken should NOT be in the serialized teacher entity (WRITE_ONLY)");
        assertNull(data.get("password"),
                "password should NOT be in the serialized teacher entity (WRITE_ONLY)");
    }

    @Test
    @Order(3)
    @DisplayName("RefreshToken: Admin login response does not expose refreshToken in data object")
    void testAdminLogin_RefreshTokenNotInData() throws Exception {
        String loginBody = objectMapper.writeValueAsString(
                Map.of("adminNo", "A001", "password", "123456"));

        String resp = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = objectMapper.readValue(resp, Map.class);

        assertNotNull(response.get("refreshToken"), "Top-level refreshToken should exist for BFF");

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertNotNull(data, "data object should exist");
        assertNull(data.get("refreshToken"),
                "refreshToken should NOT be in the serialized admin entity (WRITE_ONLY)");
        assertNull(data.get("password"),
                "password should NOT be in the serialized admin entity (WRITE_ONLY)");
    }
}