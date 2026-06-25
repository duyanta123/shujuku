package com.labcourse.filter;

import com.labcourse.entity.Student;
import com.labcourse.entity.Teacher;
import com.labcourse.repository.AdminRepository;
import com.labcourse.repository.StudentRepository;
import com.labcourse.repository.TeacherRepository;
import com.labcourse.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class JwtFilterRoleValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private AdminRepository adminRepository;

    private Long testStudentId;
    private Long testTeacherId;

    @BeforeEach
    void setUp() {
        testStudentId = studentRepository.findByStudentNo("S001").map(Student::getId).orElse(null);
        testTeacherId = teacherRepository.findByTeacherNo("T001").map(Teacher::getId).orElse(null);
    }

    @Test
    @DisplayName("角色伪造测试：学生Token伪造admin角色应被拒绝")
    void fakeAdminRoleWithStudentToken_ShouldBeRejected() throws Exception {
        if (testStudentId == null) {
            return;
        }

        String fakeAdminToken = jwtUtil.generateAccessToken(testStudentId, "S001", "admin");

        mockMvc.perform(get("/api/admin/")
                        .header("Authorization", "Bearer " + fakeAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("角色伪造测试：学生Token伪造teacher角色应被拒绝")
    void fakeTeacherRoleWithStudentToken_ShouldBeRejected() throws Exception {
        if (testStudentId == null) {
            return;
        }

        String fakeTeacherToken = jwtUtil.generateAccessToken(testStudentId, "S001", "teacher");

        mockMvc.perform(get("/api/selection/studentList/1")
                        .header("Authorization", "Bearer " + fakeTeacherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("角色伪造测试：教师Token伪造admin角色应被拒绝")
    void fakeAdminRoleWithTeacherToken_ShouldBeRejected() throws Exception {
        if (testTeacherId == null) {
            return;
        }

        String fakeAdminToken = jwtUtil.generateAccessToken(testTeacherId, "T001", "admin");

        mockMvc.perform(get("/api/admin/")
                        .header("Authorization", "Bearer " + fakeAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("角色伪造测试：教师Token伪造student角色应被拒绝")
    void fakeStudentRoleWithTeacherToken_ShouldBeRejected() throws Exception {
        if (testTeacherId == null) {
            return;
        }

        String fakeStudentToken = jwtUtil.generateAccessToken(testTeacherId, "T001", "student");

        mockMvc.perform(get("/api/selection/my")
                        .header("Authorization", "Bearer " + fakeStudentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("有效角色测试：正确角色的Token应正常工作")
    void validRoleToken_ShouldWork() throws Exception {
        if (testStudentId == null) {
            return;
        }

        String validStudentToken = jwtUtil.generateAccessToken(testStudentId, "S001", "student");

        mockMvc.perform(get("/api/selection/my")
                        .header("Authorization", "Bearer " + validStudentToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("不存在用户测试：Token中的用户ID不存在应被拒绝")
    void nonExistentUserId_ShouldBeRejected() throws Exception {
        String token = jwtUtil.generateAccessToken(999999999L, "fake", "student");

        mockMvc.perform(get("/api/selection/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}