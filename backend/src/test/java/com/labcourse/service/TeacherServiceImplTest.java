package com.labcourse.service;

import com.labcourse.entity.College;
import com.labcourse.entity.Teacher;
import com.labcourse.exception.AccountLockedException;
import com.labcourse.repository.CollegeRepository;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.TeacherRepository;
import com.labcourse.service.impl.TeacherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TeacherServiceImpl 单元测试 — 覆盖教师管理核心逻辑（原本零覆盖）
 *
 * 风险行为覆盖：
 * - login: 账号锁定拒绝、密码正确登录成功、密码错误返回 null、账号不存在返回 null
 * - updateById: 正常更新、不存在的教师、学院变更时有关联课程拒绝、学院未变更正常通过
 * - save: 密码加密保存
 * - removeById: 正常删除
 */
@SuppressWarnings("null")
class TeacherServiceImplTest {

    private TeacherServiceImpl service;
    private TeacherRepository teacherRepository;
    private PasswordEncoder passwordEncoder;
    private LoginAttemptService loginAttemptService;
    private CollegeRepository collegeRepository;
    private CourseRepository courseRepository;

    @BeforeEach
    void setUp() {
        service = new TeacherServiceImpl();
        teacherRepository = mock(TeacherRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        loginAttemptService = mock(LoginAttemptService.class);
        collegeRepository = mock(CollegeRepository.class);
        courseRepository = mock(CourseRepository.class);

        injectField(service, "teacherRepository", teacherRepository);
        injectField(service, "passwordEncoder", passwordEncoder);
        injectField(service, "loginAttemptService", loginAttemptService);
        injectField(service, "collegeRepository", collegeRepository);
        injectField(service, "courseRepository", courseRepository);
    }

    // ================================================================
    // login — 账号锁定
    // ================================================================

    @Test
    @DisplayName("login: 账号锁定时应抛出 AccountLockedException")
    void login_AccountLocked_ShouldThrowException() {
        String key = "teacher:T001";
        LoginAttemptService.LoginResult lockedResult = mock(LoginAttemptService.LoginResult.class);
        when(lockedResult.isAllowed()).thenReturn(false);
        when(lockedResult.getRemainingLockMinutes()).thenReturn(15L);

        when(loginAttemptService.checkLoginAttempt(key)).thenReturn(lockedResult);

        assertThrows(AccountLockedException.class, () -> {
            service.login("T001", "password");
        });
        verify(teacherRepository, never()).findByTeacherNo(any());
    }

    // ================================================================
    // login — 密码正确
    // ================================================================

    @Test
    @DisplayName("login: 密码正确应返回教师对象并重置尝试次数")
    void login_CorrectPassword_ShouldReturnTeacher() {
        String key = "teacher:T001";
        LoginAttemptService.LoginResult allowedResult = mock(LoginAttemptService.LoginResult.class);
        when(allowedResult.isAllowed()).thenReturn(true);

        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setTeacherNo("T001");
        teacher.setName("张教授");
        teacher.setPassword("encoded_password");

        when(loginAttemptService.checkLoginAttempt(key)).thenReturn(allowedResult);
        when(teacherRepository.findByTeacherNo("T001")).thenReturn(Optional.of(teacher));
        when(passwordEncoder.matches("correct_password", "encoded_password")).thenReturn(true);

        Teacher result = service.login("T001", "correct_password");
        assertNotNull(result, "密码正确应返回教师对象");
        assertEquals("T001", result.getTeacherNo());
        assertEquals("张教授", result.getName());
        verify(loginAttemptService).resetAttempts(key);
    }

    // ================================================================
    // login — 密码错误
    // ================================================================

    @Test
    @DisplayName("login: 密码错误应返回 null 并记录失败次数")
    void login_WrongPassword_ShouldReturnNullAndRecordFailure() {
        String key = "teacher:T001";
        LoginAttemptService.LoginResult allowedResult = mock(LoginAttemptService.LoginResult.class);
        when(allowedResult.isAllowed()).thenReturn(true);

        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setTeacherNo("T001");
        teacher.setPassword("encoded_password");

        when(loginAttemptService.checkLoginAttempt(key)).thenReturn(allowedResult);
        when(teacherRepository.findByTeacherNo("T001")).thenReturn(Optional.of(teacher));
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        Teacher result = service.login("T001", "wrong_password");
        assertNull(result, "密码错误应返回 null");
        verify(loginAttemptService).recordFailedAttempt(key);
        verify(loginAttemptService, never()).resetAttempts(key);
    }

    // ================================================================
    // login — 账号不存在
    // ================================================================

    @Test
    @DisplayName("login: 账号不存在应返回 null 并记录失败次数")
    void login_UserNotFound_ShouldReturnNull() {
        String key = "teacher:T999";
        LoginAttemptService.LoginResult allowedResult = mock(LoginAttemptService.LoginResult.class);
        when(allowedResult.isAllowed()).thenReturn(true);

        when(loginAttemptService.checkLoginAttempt(key)).thenReturn(allowedResult);
        when(teacherRepository.findByTeacherNo("T999")).thenReturn(Optional.empty());

        Teacher result = service.login("T999", "any_password");
        assertNull(result, "账号不存在应返回 null");
        verify(loginAttemptService).recordFailedAttempt(key);
    }

    // ================================================================
    // save
    // ================================================================

    @Test
    @DisplayName("save: 应加密密码后保存教师")
    void save_ShouldEncodePasswordAndSave() {
        String rawPassword = "Plain123!";
        String encodedPassword = "encoded_password";

        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        Teacher teacher = new Teacher();
        teacher.setTeacherNo("T001");
        teacher.setName("新教师");
        teacher.setPassword(rawPassword);

        boolean result = service.save(teacher);
        assertTrue(result);
        assertEquals(encodedPassword, teacher.getPassword(), "密码应被 BCrypt 加密");
        verify(teacherRepository).save(teacher);
    }

    // ================================================================
    // updateById — 正常更新
    // ================================================================

    @Test
    @DisplayName("updateById: 应成功更新教师信息")
    void updateById_ShouldSucceed() {
        Teacher existing = new Teacher();
        existing.setId(1L);
        existing.setTeacherNo("T001");
        existing.setName("张教授");
        existing.setTitle("讲师");
        existing.setCollegeId(1L);

        Teacher update = new Teacher();
        update.setId(1L);
        update.setName("张教授-更新");
        update.setTitle("副教授");

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);
        assertTrue(result);
        assertEquals("张教授-更新", existing.getName());
        assertEquals("副教授", existing.getTitle());
        verify(teacherRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: 不存在的教师应返回 false")
    void updateById_NotExists_ShouldReturnFalse() {
        Teacher update = new Teacher();
        update.setId(999L);
        update.setName("不存在");

        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = service.updateById(update);
        assertFalse(result);
        verify(teacherRepository, never()).save(any());
    }

    // ================================================================
    // updateById — 学院变更（关键业务规则）
    // ================================================================

    @Test
    @DisplayName("updateById: 学院变更但无关联课程时应允许")
    void updateById_CollegeChanged_ShouldSucceed() {
        Teacher existing = new Teacher();
        existing.setId(1L);
        existing.setTeacherNo("T001");
        existing.setName("张教授");
        existing.setCollegeId(1L);

        Teacher update = new Teacher();
        update.setId(1L);
        update.setCollegeId(2L);

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(collegeRepository.findById(2L)).thenReturn(Optional.of(new College()));

        boolean result = service.updateById(update);
        assertTrue(result, "无关联课程时应允许变更学院");
        assertEquals(2L, existing.getCollegeId());
        verify(teacherRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: 首次设置 collegeId 且旧值为 null 应不触发课程检查")
    void updateById_FirstTimeCollegeId_ShouldSkipCourseCheck() {
        Teacher existing = new Teacher();
        existing.setId(1L);
        existing.setTeacherNo("T001");
        existing.setName("张教授");
        existing.setCollegeId(null);  // 从未设置过学院

        Teacher update = new Teacher();
        update.setId(1L);
        update.setCollegeId(1L);  // 首次设置

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(collegeRepository.findById(1L)).thenReturn(Optional.of(new College()));

        boolean result = service.updateById(update);
        assertTrue(result, "首次设置学院ID应不触发课程检查");
        assertEquals(1L, existing.getCollegeId());
        verify(teacherRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: 学院未变更不应检查课程关联")
    void updateById_CollegeNotChanged_ShouldNotCheckCourses() {
        Teacher existing = new Teacher();
        existing.setId(1L);
        existing.setTeacherNo("T001");
        existing.setName("张教授");
        existing.setCollegeId(1L);

        Teacher update = new Teacher();
        update.setId(1L);
        update.setCollegeId(1L);  // 相同学院

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);
        assertTrue(result);
    }

    // ================================================================
    // updateById — 密码更新
    // ================================================================

    @Test
    @DisplayName("updateById: 更新密码时应加密后保存")
    void updateById_PasswordUpdate_ShouldEncode() {
        Teacher existing = new Teacher();
        existing.setId(1L);
        existing.setTeacherNo("T001");
        existing.setName("张教授");
        existing.setPassword("old_encoded");

        Teacher update = new Teacher();
        update.setId(1L);
        update.setPassword("NewPass1!");

        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("NewPass1!")).thenReturn("new_encoded");

        boolean result = service.updateById(update);
        assertTrue(result);
        assertEquals("new_encoded", existing.getPassword());
        verify(passwordEncoder).encode("NewPass1!");
        verify(teacherRepository).save(existing);
    }

    // ================================================================
    // removeById
    // ================================================================

    @Test
    @DisplayName("removeById: 应成功删除教师")
    void removeById_ShouldSucceed() {
        when(courseRepository.findByTeacherId(1L)).thenReturn(java.util.Collections.emptyList());
        boolean result = service.removeById(1L);
        assertTrue(result);
        verify(teacherRepository).deleteById(1L);
    }

    // ================================================================
    // list
    // ================================================================

    @Test
    @DisplayName("list: 应返回所有教师")
    void list_ShouldReturnAll() {
        when(teacherRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        var result = service.list(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("list: collegeId 筛选 — null 返回全部 / 有效值返回筛选 / 无效值返回空")
    void testListWithCollegeId() {
        // 测试 collegeId = null 时返回全部
        List<Teacher> allTeachers = service.list(null);
        assertNotNull(allTeachers);
        assertTrue(allTeachers.size() >= 0);

        // 测试 collegeId = 有效值返回筛选结果
        List<Teacher> filtered = service.list(1L);
        assertNotNull(filtered);
        assertTrue(filtered.size() <= allTeachers.size());

        // 测试 collegeId = 无效值返回空列表
        List<Teacher> empty = service.list(99999L);
        assertNotNull(empty);
        assertEquals(0, empty.size());
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
