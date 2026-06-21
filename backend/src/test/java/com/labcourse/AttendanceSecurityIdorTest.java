package com.labcourse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labcourse.entity.Course;
import com.labcourse.repository.CourseRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for Teacher IDOR vulnerabilities (MEDIUM).
 * Verifies that a teacher cannot access attendance/student data for courses they don't teach.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("null")
class AttendanceSecurityIdorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    private String teacherToken;
    private Long teacherId = 1L; // T001
    private Long nonOwnedCourseId;
    private LocalDate today;

    @BeforeEach
    void setUp() throws Exception {
        today = LocalDate.now();

        // Login as T001
        String loginBody = objectMapper.writeValueAsString(
                Map.of("teacherNo", "T001", "password", "123456"));
        String resp = mockMvc.perform(post("/api/teacher/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        teacherToken = (String) objectMapper.readValue(resp, Map.class).get("accessToken");

        // Find a course NOT taught by T001
        List<Course> allCourses = courseRepository.findAll();
        nonOwnedCourseId = allCourses.stream()
                .filter(c -> !c.getTeacherId().equals(teacherId))
                .map(Course::getId)
                .findFirst()
                .orElse(null);

        if (nonOwnedCourseId == null) {
            // Fallback: use a course ID that is unlikely to be owned by T001
            nonOwnedCourseId = allCourses.stream()
                    .filter(c -> c.getTeacherId() != null)
                    .map(Course::getId)
                    .max(Long::compareTo)
                    .orElse(2L);
        }
    }

    // ================================================================
    // FINDING 1: Teacher IDOR - Cross-Course Data Access
    // ================================================================

    @Test
    @Order(1)
    @DisplayName("IDOR: Teacher cannot access course attendance for non-owned course")
    void testGetCourseAttendance_NonOwnedCourse_Forbidden() throws Exception {
        assertNotNull(nonOwnedCourseId, "Should find a non-owned course for testing");

        mockMvc.perform(get("/api/attendance/course")
                        .param("courseId", nonOwnedCourseId.toString())
                        .param("date", today.toString())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("无权访问此课程的考勤数据"));
    }

    @Test
    @Order(2)
    @DisplayName("IDOR: Teacher cannot access attendance dates for non-owned course")
    void testGetAttendanceDates_NonOwnedCourse_Forbidden() throws Exception {
        assertNotNull(nonOwnedCourseId, "Should find a non-owned course for testing");

        mockMvc.perform(get("/api/attendance/dates")
                        .param("courseId", nonOwnedCourseId.toString())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("无权访问此课程的考勤数据"));
    }

    @Test
    @Order(3)
    @DisplayName("IDOR: Teacher cannot export attendance for non-owned course")
    void testExportAttendance_NonOwnedCourse_Forbidden() throws Exception {
        assertNotNull(nonOwnedCourseId, "Should find a non-owned course for testing");

        mockMvc.perform(get("/api/attendance/export")
                        .param("courseId", nonOwnedCourseId.toString())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("无权访问此课程的考勤数据"));
    }

    @Test
    @Order(4)
    @DisplayName("IDOR: Teacher cannot access student list for non-owned course")
    void testGetStudentList_NonOwnedCourse_Forbidden() throws Exception {
        assertNotNull(nonOwnedCourseId, "Should find a non-owned course for testing");

        mockMvc.perform(get("/api/selection/studentList/" + nonOwnedCourseId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("无权访问此课程的学生列表"));
    }

    // ================================================================
    // FINDING 3: Attendance status modification without course ownership
    // ================================================================

    @Test
    @Order(5)
    @DisplayName("IDOR: Teacher cannot modify attendance status for non-owned course's record")
    void testUpdateAttendanceStatus_NonOwnedCourse_Forbidden() throws Exception {
        assertNotNull(nonOwnedCourseId, "Should find a non-owned course for testing");

        // Try to update an attendance record belonging to a course T001 doesn't teach
        String updateBody = objectMapper.writeValueAsString(Map.of(
                "attendanceId", 1L,  // arbitrary attendance ID
                "newStatus", "请假",
                "reason", "Unauthorized attempt"));

        // This should be rejected because the attendance record's course is not taught by T001
        // (unless attendanceId=1 happens to belong to T001's course, which is unlikely
        //  since nonOwnedCourseId is guaranteed to not be T001's)
        mockMvc.perform(put("/api/attendance/update-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + teacherToken)
                        .content(updateBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}