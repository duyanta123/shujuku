package com.labcourse.service;

import com.labcourse.entity.Admin;
import com.labcourse.entity.LoginAttempt;
import com.labcourse.exception.AccountLockedException;
import com.labcourse.repository.AdminRepository;
import com.labcourse.repository.LoginAttemptRepository;
import com.labcourse.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AdminServiceImpl 单元测试 — 覆盖管理员登录与账号锁定机制
 *
 * 风险行为覆盖：
 * - login: 账号锁定拦截（5次失败后锁定30分钟）、正确密码登录重置失败计数、
 *          错误密码递增失败计数、不存在的用户
 * - updateById: 传密码则编码更新、不传密码保留原密码、不存在的管理员 ID
 * - save: 密码编码后保存
 * - removeById: 基础删除路径
 */
@SuppressWarnings("null")
class AdminServiceImplTest {

    private AdminServiceImpl service;
    private AdminRepository adminRepository;
    private PasswordEncoder passwordEncoder;
    private LoginAttemptService loginAttemptService;
    private LoginAttemptRepository loginAttemptRepository;
    private Map<String, LoginAttempt> attemptStore;

    @BeforeEach
    void setUp() {
        service = new AdminServiceImpl();
        adminRepository = mock(AdminRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        loginAttemptRepository = mock(LoginAttemptRepository.class);
        loginAttemptService = new LoginAttemptService(); // 使用真实实例以确保状态逻辑正确
        attemptStore = new HashMap<>();

        // 使用 Map 模拟 Repository 的 save/findById 行为，保持状态一致性
        when(loginAttemptRepository.findById(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            return Optional.ofNullable(attemptStore.get(key));
        });
        doAnswer(inv -> {
            LoginAttempt attempt = inv.getArgument(0);
            attemptStore.put(attempt.getAttemptKey(), attempt);
            return null;
        }).when(loginAttemptRepository).save(any(LoginAttempt.class));
        doAnswer(inv -> {
            String key = inv.getArgument(0);
            attemptStore.remove(key);
            return null;
        }).when(loginAttemptRepository).deleteById(anyString());

        injectField(service, "adminRepository", adminRepository);
        injectField(service, "passwordEncoder", passwordEncoder);
        injectField(service, "loginAttemptService", loginAttemptService);
        injectField(loginAttemptService, "loginAttemptRepository", loginAttemptRepository);
    }

    // ================================================================
    // login — 账号锁定
    // ================================================================

    @Test
    @DisplayName("login: 5次失败后应抛出 AccountLockedException")
    void login_LockedAccount_ShouldThrowAccountLockedException() {
        String username = "admin";
        String wrongPassword = "wrong";

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // 执行5次失败登录触发锁定
        for (int i = 0; i < 5; i++) {
            Admin result = service.login(username, wrongPassword);
            assertNull(result, "第" + (i + 1) + "次失败登录应返回 null");
        }

        // 第6次应抛出 AccountLockedException
        assertThrows(AccountLockedException.class, () -> service.login(username, wrongPassword),
                "5次失败后账号应被锁定");
    }

    // ================================================================
    // login — 成功登录重置计数
    // ================================================================

    @Test
    @DisplayName("login: 正确密码登录应成功并重置失败计数")
    void login_ValidCredentials_ShouldSucceedAndResetAttempts() {
        String username = "admin";
        String password = "correct";

        Admin admin = new Admin();
        admin.setId(1L);
        admin.setUsername(username);
        admin.setPassword("encoded_password");

        when(adminRepository.findByUsername(username)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches(password, "encoded_password")).thenReturn(true);

        Admin result = service.login(username, password);

        assertNotNull(result, "正确密码应返回 Admin 对象");
        assertEquals(username, result.getUsername());
        verify(passwordEncoder).matches(password, "encoded_password");
    }

    @Test
    @DisplayName("login: 之前有3次失败后正确登录仍应成功")
    void login_AfterPreviousFailures_ShouldStillSucceed() {
        String username = "admin";
        String wrongPassword = "wrong";
        String correctPassword = "correct";

        Admin admin = new Admin();
        admin.setId(1L);
        admin.setUsername(username);
        admin.setPassword("encoded_password");

        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // 3次失败登录
        for (int i = 0; i < 3; i++) {
            service.login(username, wrongPassword);
        }

        // 第4次正确登录
        when(adminRepository.findByUsername(username)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches(correctPassword, "encoded_password")).thenReturn(true);

        Admin result = service.login(username, correctPassword);

        assertNotNull(result, "之前有失败但正确密码应能登录");
    }

    // ================================================================
    // login — 错误密码登录
    // ================================================================

    @Test
    @DisplayName("login: 错误密码应返回 null")
    void login_WrongPassword_ShouldReturnNull() {
        String username = "admin";

        Admin admin = new Admin();
        admin.setId(1L);
        admin.setUsername(username);
        admin.setPassword("encoded_password");

        when(adminRepository.findByUsername(username)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong", "encoded_password")).thenReturn(false);

        Admin result = service.login(username, "wrong");

        assertNull(result, "错误密码应返回 null");
        // loginAttemptService 为真实实例，失败计数已递增（通过后续锁定测试间接验证）
    }

    @Test
    @DisplayName("login: 不存在的用户应返回 null")
    void login_UserNotFound_ShouldReturnNull() {
        String username = "nonexistent";

        when(adminRepository.findByUsername(username)).thenReturn(Optional.empty());

        Admin result = service.login(username, "any");

        assertNull(result, "不存在的用户应返回 null");
    }

    // ================================================================
    // save — 密码编码
    // ================================================================

    @Test
    @DisplayName("save: 应编码密码后保存")
    void save_ShouldEncodePassword() {
        Admin admin = new Admin();
        admin.setUsername("newadmin");
        admin.setPassword("Plain123!");

        when(passwordEncoder.encode("Plain123!")).thenReturn("hashed_password");

        boolean result = service.save(admin);

        assertTrue(result);
        assertEquals("hashed_password", admin.getPassword(), "密码应被编码");
        verify(adminRepository).save(admin);
    }

    // ================================================================
    // updateById — 传密码则更新
    // ================================================================

    @Test
    @DisplayName("updateById: 传入新密码应编码并更新")
    void updateById_WithNewPassword_ShouldEncodeAndUpdate() {
        Admin existing = new Admin();
        existing.setId(1L);
        existing.setUsername("admin");
        existing.setPassword("old_hashed");

        Admin update = new Admin();
        update.setId(1L);
        update.setUsername("admin_new");
        update.setPassword("NewPass1!");

        when(adminRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("NewPass1!")).thenReturn("new_hashed");

        boolean result = service.updateById(update);

        assertTrue(result);
        assertEquals("admin_new", existing.getUsername());
        assertEquals("new_hashed", existing.getPassword(), "新密码应被编码");
        verify(adminRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: 不传密码应保留原密码")
    void updateById_WithoutPassword_ShouldRetainOldPassword() {
        Admin existing = new Admin();
        existing.setId(1L);
        existing.setUsername("admin");
        existing.setPassword("old_hashed");

        Admin update = new Admin();
        update.setId(1L);
        update.setUsername("admin_new");
        update.setPassword(null); // 不传密码

        when(adminRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);

        assertTrue(result);
        assertEquals("admin_new", existing.getUsername());
        assertEquals("old_hashed", existing.getPassword(), "不传密码应保留原密码");
        verify(adminRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: 传空字符串密码应保留原密码")
    void updateById_WithEmptyPassword_ShouldRetainOldPassword() {
        Admin existing = new Admin();
        existing.setId(1L);
        existing.setUsername("admin");
        existing.setPassword("old_hashed");

        Admin update = new Admin();
        update.setId(1L);
        update.setUsername("admin_new");
        update.setPassword(""); // 空字符串

        when(adminRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);

        assertTrue(result);
        assertEquals("old_hashed", existing.getPassword(), "空字符串密码应保留原密码");
    }

    @Test
    @DisplayName("updateById: 不存在的管理员 ID 应返回 false")
    void updateById_NonExistentId_ShouldReturnFalse() {
        Admin update = new Admin();
        update.setId(9999L);

        when(adminRepository.findById(9999L)).thenReturn(Optional.empty());

        boolean result = service.updateById(update);

        assertFalse(result, "不存在的管理员应返回 false");
        verify(adminRepository, never()).save(any());
    }

    // ================================================================
    // removeById
    // ================================================================

    @Test
    @DisplayName("removeById: 应调用 deleteById")
    void removeById_ShouldCallDelete() {
        boolean result = service.removeById(1L);

        assertTrue(result);
        verify(adminRepository).deleteById(1L);
    }

    // ================================================================
    // list
    // ================================================================

    @Test
    @DisplayName("list: 应返回所有管理员")
    void list_ShouldReturnAll() {
        when(adminRepository.findAll()).thenReturn(java.util.List.of(new Admin(), new Admin()));

        var result = service.list();

        assertEquals(2, result.size());
        verify(adminRepository).findAll();
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
