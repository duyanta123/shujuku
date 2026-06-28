package com.labcourse.controller;

import com.labcourse.entity.Admin;
import com.labcourse.entity.Student;
import com.labcourse.entity.Teacher;
import com.labcourse.repository.AdminRepository;
import com.labcourse.repository.StudentRepository;
import com.labcourse.repository.TeacherRepository;
import com.labcourse.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AuthController 单元测试 — 覆盖 Token 轮转与登出的安全关键路径
 *
 * 风险行为覆盖：
 * - validateToken: 有效 Token、过期/无效 Token、缺少 Authorization 头、
 *              用户不存在（student/teacher/admin 三种角色验证）
 * - refreshToken: Token 轮转（旧 Refresh Token 失效 + 新 pair 生成）、
 *              Refresh Token 已使用（重放攻击）、无效 Refresh Token、
 *              缺少参数、三种角色（student/teacher/admin）
 * - logout: 清除 Refresh Token、无 Authorization 头
 *
 * 注：因 Mockito 在 Java 25 下无法 mock JwtUtil，使用手动 TestDouble 替代。
 */
@SuppressWarnings("null")
class AuthControllerTest {

    private AuthController controller;
    private TestJwtUtil jwtUtil;
    private StudentRepository studentRepository;
    private TeacherRepository teacherRepository;
    private AdminRepository adminRepository;

    // ================================================================
    // JwtUtil 的测试替身 — 避免 Mockito 在 Java 25 下的兼容性问题
    // ================================================================

    static class TestJwtUtil extends JwtUtil {
        // 通过反射设置 secret 字段避免 NPE
        TestJwtUtil() {
            try {
                var field = JwtUtil.class.getDeclaredField("secret");
                field.setAccessible(true);
                field.set(this, "test-secret-for-unit-tests-at-least-256-bits-long-enough");
            } catch (Exception ignored) {
                // fallback
            }
        }

        // ===== 可配置的 stub 方法 =====
        private Claims validateRefreshTokenResult;
        private Claims validateAccessTokenResult;
        private String generateAccessTokenResult;
        private String generateRefreshTokenResult;
        private Claims parseTokenResult;

        void setValidateRefreshTokenResult(Claims result) { this.validateRefreshTokenResult = result; }
        void setValidateAccessTokenResult(Claims result) { this.validateAccessTokenResult = result; }
        void setGenerateAccessTokenResult(String result) { this.generateAccessTokenResult = result; }
        void setGenerateRefreshTokenResult(String result) { this.generateRefreshTokenResult = result; }
        void setParseTokenResult(Claims result) { this.parseTokenResult = result; }

        @Override
        public Claims validateRefreshToken(String token) { return validateRefreshTokenResult; }

        @Override
        public Claims validateAccessToken(String token) { return validateAccessTokenResult; }

        @Override
        public String generateAccessToken(Long userId, String username, String role) { return generateAccessTokenResult; }

        @Override
        public String generateRefreshToken(Long userId) { return generateRefreshTokenResult; }

        @Override
        public Claims parseToken(String token) { return parseTokenResult; }
    }

    // ================================================================

    @BeforeEach
    void setUp() {
        controller = new AuthController();
        jwtUtil = new TestJwtUtil();
        studentRepository = mock(StudentRepository.class);
        teacherRepository = mock(TeacherRepository.class);
        adminRepository = mock(AdminRepository.class);

        injectField(controller, "jwtUtil", jwtUtil);
        injectField(controller, "studentRepository", studentRepository);
        injectField(controller, "teacherRepository", teacherRepository);
        injectField(controller, "adminRepository", adminRepository);
    }

    /**
     * 创建一个 Claims 对象用于测试。
     * 使用 Jwts.claims() builder，在 build 前设置属性以避免不可变问题。
     */
    private static Claims createClaimsStub(String subject, String... role) {
        var builder = io.jsonwebtoken.Jwts.claims().subject(subject);
        if (role.length > 0 && role[0] != null) {
            builder.add("role", role[0]);
        }
        return builder.build();
    }

    /**
     * 创建一个带 username 的 Claims 对象用于测试。
     */
    private static Claims createClaimsStub(String subject, String role, String username) {
        var builder = io.jsonwebtoken.Jwts.claims().subject(subject);
        if (role != null) {
            builder.add("role", role);
        }
        if (username != null) {
            builder.add("username", username);
        }
        return builder.build();
    }

    // ================================================================
    // validateToken — 新增端点（commit 4274e4f）
    // ================================================================

    @Test
    @DisplayName("validateToken: 有效 Student Token 应返回用户信息")
    void validateToken_ValidStudent_ShouldReturnUserInfo() {
        Claims claims = createClaimsStub("1", "student", "test_student");
        jwtUtil.setValidateAccessTokenResult(claims);

        Student student = new Student();
        student.setId(1L);
        when(studentRepository.existsById(1L)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.validateToken("Bearer valid_token");

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("Token有效", body.get("message"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertEquals(1L, data.get("userId"));
        assertEquals("test_student", data.get("username"));
        assertEquals("student", data.get("role"));
    }

    @Test
    @DisplayName("validateToken: 有效 Teacher Token 应返回用户信息")
    void validateToken_ValidTeacher_ShouldReturnUserInfo() {
        Claims claims = createClaimsStub("2", "teacher", "test_teacher");
        jwtUtil.setValidateAccessTokenResult(claims);

        when(teacherRepository.existsById(2L)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.validateToken("Bearer valid_token");

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertEquals("teacher", data.get("role"));
    }

    @Test
    @DisplayName("validateToken: 有效 Admin Token 应返回用户信息")
    void validateToken_ValidAdmin_ShouldReturnUserInfo() {
        Claims claims = createClaimsStub("3", "admin", "test_admin");
        jwtUtil.setValidateAccessTokenResult(claims);

        when(adminRepository.existsById(3L)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.validateToken("Bearer valid_token");

        assertEquals(200, response.getStatusCode().value());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertEquals("admin", data.get("role"));
    }

    @Test
    @DisplayName("validateToken: 缺少 Authorization 头应返回 401")
    void validateToken_MissingAuthHeader_ShouldReturn401() {
        ResponseEntity<Map<String, Object>> response = controller.validateToken(null);

        assertEquals(401, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertTrue(((String) body.get("message")).contains("缺少"));
    }

    @Test
    @DisplayName("validateToken: Authorization 头不以 Bearer 开头应返回 401")
    void validateToken_InvalidAuthHeaderFormat_ShouldReturn401() {
        ResponseEntity<Map<String, Object>> response = controller.validateToken("Basic abc123");

        assertEquals(401, response.getStatusCode().value());
        assertFalse((Boolean) response.getBody().get("success"));
    }

    @Test
    @DisplayName("validateToken: 过期或无效 Token 应返回 401")
    void validateToken_ExpiredToken_ShouldReturn401() {
        jwtUtil.setValidateAccessTokenResult(null);

        ResponseEntity<Map<String, Object>> response = controller.validateToken("Bearer expired_token");

        assertEquals(401, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertTrue(((String) body.get("message")).contains("过期"));
    }

    @Test
    @DisplayName("validateToken: Token 有效但用户不存在应返回 401")
    void validateToken_UserNotExists_ShouldReturn401() {
        Claims claims = createClaimsStub("999", "student", "ghost_user");
        jwtUtil.setValidateAccessTokenResult(claims);

        when(studentRepository.existsById(999L)).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.validateToken("Bearer valid_token");

        assertEquals(401, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("用户不存在", body.get("message"));
    }

    @Test
    @DisplayName("validateToken: Token 中 role 为 null 时 userExists 应返回 false")
    void validateToken_NullRole_ShouldReturn401() {
        Claims claims = createClaimsStub("1");
        // role 未设置，应为 null
        jwtUtil.setValidateAccessTokenResult(claims);

        ResponseEntity<Map<String, Object>> response = controller.validateToken("Bearer valid_token");

        assertEquals(401, response.getStatusCode().value());
        assertEquals("用户不存在", response.getBody().get("message"));
    }

    @Test
    @DisplayName("validateToken: 未知 role 的 userExists 应返回 false")
    void validateToken_UnknownRole_ShouldReturn401() {
        Claims claims = createClaimsStub("1", "superadmin", "unknown");
        jwtUtil.setValidateAccessTokenResult(claims);

        ResponseEntity<Map<String, Object>> response = controller.validateToken("Bearer valid_token");

        assertEquals(401, response.getStatusCode().value());
        assertEquals("用户不存在", response.getBody().get("message"));
    }

    // ================================================================
    // refreshToken — 成功（Student）
    // ================================================================

    @Test
    @DisplayName("refreshToken: 学生角色的 Token 轮转应成功")
    void refreshToken_Student_ShouldRotateTokens() {
        String oldRefreshToken = "old_refresh_student";
        String newAccessToken = "new_access";
        String newRefreshToken = "new_refresh";

        Claims claims = createClaimsStub("1");
        jwtUtil.setValidateRefreshTokenResult(claims);
        jwtUtil.setGenerateAccessTokenResult(newAccessToken);
        jwtUtil.setGenerateRefreshTokenResult(newRefreshToken);

        Student student = new Student();
        student.setId(1L);
        student.setStudentNo("S001");
        student.setRefreshToken(oldRefreshToken);

        when(studentRepository.findByRefreshToken(oldRefreshToken)).thenReturn(Optional.of(student));

        ResponseEntity<Map<String, Object>> response = controller.refreshToken(
                Map.of("refreshToken", oldRefreshToken));

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals(newAccessToken, body.get("accessToken"));
        assertEquals(newRefreshToken, body.get("refreshToken"));

        verify(studentRepository).save(argThat(s -> newRefreshToken.equals(s.getRefreshToken())));
    }

    // ================================================================
    // refreshToken — 成功（Teacher）
    // ================================================================

    @Test
    @DisplayName("refreshToken: 教师角色的 Token 轮转应成功")
    void refreshToken_Teacher_ShouldRotateTokens() {
        String oldRefreshToken = "old_refresh_teacher";
        String newAccessToken = "new_access_t";
        String newRefreshToken = "new_refresh_t";

        Claims claims = createClaimsStub("2");
        jwtUtil.setValidateRefreshTokenResult(claims);
        jwtUtil.setGenerateAccessTokenResult(newAccessToken);
        jwtUtil.setGenerateRefreshTokenResult(newRefreshToken);

        Teacher teacher = new Teacher();
        teacher.setId(2L);
        teacher.setTeacherNo("T001");
        teacher.setRefreshToken(oldRefreshToken);

        when(teacherRepository.findByRefreshToken(oldRefreshToken)).thenReturn(Optional.of(teacher));
        when(studentRepository.findByRefreshToken(oldRefreshToken)).thenReturn(Optional.empty());
        when(adminRepository.findByRefreshToken(oldRefreshToken)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.refreshToken(
                Map.of("refreshToken", oldRefreshToken));

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        verify(teacherRepository).save(argThat(t -> newRefreshToken.equals(t.getRefreshToken())));
    }

    // ================================================================
    // refreshToken — 成功（Admin）
    // ================================================================

    @Test
    @DisplayName("refreshToken: 管理员角色的 Token 轮转应成功")
    void refreshToken_Admin_ShouldRotateTokens() {
        String oldRefreshToken = "old_refresh_admin";
        String newAccessToken = "new_access_a";
        String newRefreshToken = "new_refresh_a";

        Claims claims = createClaimsStub("3");
        jwtUtil.setValidateRefreshTokenResult(claims);
        jwtUtil.setGenerateAccessTokenResult(newAccessToken);
        jwtUtil.setGenerateRefreshTokenResult(newRefreshToken);

        Admin admin = new Admin();
        admin.setId(3L);
        admin.setUsername("admin");
        admin.setRefreshToken(oldRefreshToken);

        when(adminRepository.findByRefreshToken(oldRefreshToken)).thenReturn(Optional.of(admin));
        when(studentRepository.findByRefreshToken(oldRefreshToken)).thenReturn(Optional.empty());
        when(teacherRepository.findByRefreshToken(oldRefreshToken)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.refreshToken(
                Map.of("refreshToken", oldRefreshToken));

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        verify(adminRepository).save(argThat(a -> newRefreshToken.equals(a.getRefreshToken())));
    }

    // ================================================================
    // refreshToken — Refresh Token 已使用（重放攻击）
    // ================================================================

    @Test
    @DisplayName("refreshToken: 已轮转过的旧 Refresh Token 应拒绝（防重放）")
    void refreshToken_UsedRefreshToken_ShouldReject() {
        String usedRefreshToken = "already_rotated_token";

        Claims claims = createClaimsStub("1");
        jwtUtil.setValidateRefreshTokenResult(claims);

        when(studentRepository.findByRefreshToken(usedRefreshToken)).thenReturn(Optional.empty());
        when(teacherRepository.findByRefreshToken(usedRefreshToken)).thenReturn(Optional.empty());
        when(adminRepository.findByRefreshToken(usedRefreshToken)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.refreshToken(
                Map.of("refreshToken", usedRefreshToken));

        assertEquals(401, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertTrue(((String) body.get("message")).contains("已被使用"));
    }

    // ================================================================
    // refreshToken — 无效 Refresh Token
    // ================================================================

    @Test
    @DisplayName("refreshToken: 无效的 Refresh Token 应返回 401")
    void refreshToken_InvalidToken_ShouldReturn401() {
        String invalidToken = "invalid_refresh";

        jwtUtil.setValidateRefreshTokenResult(null);

        ResponseEntity<Map<String, Object>> response = controller.refreshToken(
                Map.of("refreshToken", invalidToken));

        assertEquals(401, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertTrue(((String) body.get("message")).contains("无效"));
    }

    // ================================================================
    // refreshToken — 缺少参数
    // ================================================================

    @Test
    @DisplayName("refreshToken: 缺少 refreshToken 参数应返回 400")
    void refreshToken_MissingParameter_ShouldReturn400() {
        // null — 由于 Map.of 不接受 null value，使用 HashMap
        ResponseEntity<Map<String, Object>> response1 = controller.refreshToken(
                new java.util.HashMap<>() {{ put("refreshToken", null); }});
        assertEquals(400, response1.getStatusCode().value());

        ResponseEntity<Map<String, Object>> response2 = controller.refreshToken(
                Map.of("refreshToken", ""));
        assertEquals(400, response2.getStatusCode().value());
    }

    // ================================================================
    // logout — 清除 Refresh Token
    // ================================================================

    @Test
    @DisplayName("logout: 学生角色登出应清除 Refresh Token")
    void logout_Student_ShouldClearRefreshToken() {
        String accessToken = "valid_access_token";
        Claims claims = createClaimsStub("1", "student");

        jwtUtil.setValidateAccessTokenResult(claims);

        Student student = new Student();
        student.setId(1L);
        student.setRefreshToken("some_refresh");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        ResponseEntity<Map<String, Object>> response = controller.logout("Bearer " + accessToken);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertNull(student.getRefreshToken(), "登出后 Refresh Token 应为 null");
        verify(studentRepository).save(student);
    }

    @Test
    @DisplayName("logout: 教师角色登出应清除 Refresh Token")
    void logout_Teacher_ShouldClearRefreshToken() {
        String accessToken = "valid_access_token";
        Claims claims = createClaimsStub("2", "teacher");

        jwtUtil.setValidateAccessTokenResult(claims);

        Teacher teacher = new Teacher();
        teacher.setId(2L);
        teacher.setRefreshToken("some_refresh");

        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher));

        ResponseEntity<Map<String, Object>> response = controller.logout("Bearer " + accessToken);

        assertEquals(200, response.getStatusCode().value());
        assertNull(teacher.getRefreshToken());
        verify(teacherRepository).save(teacher);
    }

    @Test
    @DisplayName("logout: 管理员角色登出应清除 Refresh Token")
    void logout_Admin_ShouldClearRefreshToken() {
        String accessToken = "valid_access_token";
        Claims claims = createClaimsStub("3", "admin");

        jwtUtil.setValidateAccessTokenResult(claims);

        Admin admin = new Admin();
        admin.setId(3L);
        admin.setRefreshToken("some_refresh");

        when(adminRepository.findById(3L)).thenReturn(Optional.of(admin));

        ResponseEntity<Map<String, Object>> response = controller.logout("Bearer " + accessToken);

        assertEquals(200, response.getStatusCode().value());
        assertNull(admin.getRefreshToken());
        verify(adminRepository).save(admin);
    }

    @Test
    @DisplayName("logout: 无 Authorization 头应仍返回成功")
    void logout_NoAuthHeader_ShouldReturn401() {
        ResponseEntity<Map<String, Object>> response = controller.logout(null);

        assertEquals(401, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
    }

    // ===== 工具方法 =====

    private static void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            fail("无法注入 " + fieldName + ": " + e.getMessage());
        }
    }
}
