package com.labcourse.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ProductionConfigValidator 单元测试 — 覆盖生产环境数据库凭证校验
 *
 * 风险行为覆盖：
 * - 非生产环境：不执行校验，不抛异常
 * - 生产环境：凭证为空抛异常
 * - 生产环境：用户名为 root 抛异常
 * - 生产环境：密码为 demo 凭证抛异常
 * - 生产环境：合法凭证不抛异常
 */
@DisplayName("ProductionConfigValidator 生产环境配置校验")
class ProductionConfigValidatorTest {

    // ================================================================
    // 非生产环境 — 跳过校验
    // ================================================================

    @Test
    @DisplayName("非 prod 环境不执行校验")
    void nonProdProfile_ShouldSkipValidation() {
        Environment env = mock(Environment.class);
        when(env.matchesProfiles("prod")).thenReturn(false);

        ProductionConfigValidator validator = new ProductionConfigValidator(env);
        assertDoesNotThrow(() -> validator.run(null));
    }

    // ================================================================
    // 生产环境 — 凭证为空
    // ================================================================

    @Test
    @DisplayName("生产环境: username 为空抛 IllegalStateException")
    void prodProfile_UsernameNull_ShouldThrow() {
        Environment env = mock(Environment.class);
        when(env.matchesProfiles("prod")).thenReturn(true);
        when(env.getProperty("spring.datasource.username")).thenReturn(null);
        when(env.getProperty("spring.datasource.password")).thenReturn("securePass123");

        ProductionConfigValidator validator = new ProductionConfigValidator(env);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(null));
        assertTrue(ex.getMessage().contains("credentials"));
    }

    @Test
    @DisplayName("生产环境: password 为空抛 IllegalStateException")
    void prodProfile_PasswordNull_ShouldThrow() {
        Environment env = mock(Environment.class);
        when(env.matchesProfiles("prod")).thenReturn(true);
        when(env.getProperty("spring.datasource.username")).thenReturn("app_user");
        when(env.getProperty("spring.datasource.password")).thenReturn(null);

        ProductionConfigValidator validator = new ProductionConfigValidator(env);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(null));
        assertTrue(ex.getMessage().contains("credentials"));
    }

    @Test
    @DisplayName("生产环境: username 为空白字符串抛 IllegalStateException")
    void prodProfile_UsernameBlank_ShouldThrow() {
        Environment env = mock(Environment.class);
        when(env.matchesProfiles("prod")).thenReturn(true);
        when(env.getProperty("spring.datasource.username")).thenReturn("   ");
        when(env.getProperty("spring.datasource.password")).thenReturn("securePass123");

        ProductionConfigValidator validator = new ProductionConfigValidator(env);
        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    // ================================================================
    // 生产环境 — 用户名 root
    // ================================================================

    @Test
    @DisplayName("生产环境: 用户名为 root 抛 IllegalStateException")
    void prodProfile_RootUser_ShouldThrow() {
        Environment env = mock(Environment.class);
        when(env.matchesProfiles("prod")).thenReturn(true);
        when(env.getProperty("spring.datasource.username")).thenReturn("root");
        when(env.getProperty("spring.datasource.password")).thenReturn("complexPass!@#");

        ProductionConfigValidator validator = new ProductionConfigValidator(env);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(null));
        assertTrue(ex.getMessage().contains("root"));
    }

    @Test
    @DisplayName("生产环境: 用户名为 ROOT(大小写不敏感) 抛 IllegalStateException")
    void prodProfile_RootUserCaseInsensitive_ShouldThrow() {
        Environment env = mock(Environment.class);
        when(env.matchesProfiles("prod")).thenReturn(true);
        when(env.getProperty("spring.datasource.username")).thenReturn("ROOT");
        when(env.getProperty("spring.datasource.password")).thenReturn("complexPass!@#");

        ProductionConfigValidator validator = new ProductionConfigValidator(env);
        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    // ================================================================
    // 生产环境 — 弱密码
    // ================================================================

    @Test
    @DisplayName("生产环境: 密码为 123456 抛 IllegalStateException")
    void prodProfile_DemoPassword123456_ShouldThrow() {
        Environment env = mock(Environment.class);
        when(env.matchesProfiles("prod")).thenReturn(true);
        when(env.getProperty("spring.datasource.username")).thenReturn("app_user");
        when(env.getProperty("spring.datasource.password")).thenReturn("123456");

        ProductionConfigValidator validator = new ProductionConfigValidator(env);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(null));
        assertTrue(ex.getMessage().contains("demo"));
    }

    @Test
    @DisplayName("生产环境: 密码为 demo(大小写不敏感) 抛 IllegalStateException")
    void prodProfile_DemoPasswordDemo_ShouldThrow() {
        Environment env = mock(Environment.class);
        when(env.matchesProfiles("prod")).thenReturn(true);
        when(env.getProperty("spring.datasource.username")).thenReturn("app_user");
        when(env.getProperty("spring.datasource.password")).thenReturn("DEMO");

        ProductionConfigValidator validator = new ProductionConfigValidator(env);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.run(null));
        assertTrue(ex.getMessage().contains("demo"));
    }

    // ================================================================
    // 生产环境 — 合法凭证
    // ================================================================

    @Test
    @DisplayName("生产环境: 合法凭证不抛异常")
    void prodProfile_ValidCredentials_ShouldNotThrow() {
        Environment env = mock(Environment.class);
        when(env.matchesProfiles("prod")).thenReturn(true);
        when(env.getProperty("spring.datasource.username")).thenReturn("app_prod_user");
        when(env.getProperty("spring.datasource.password")).thenReturn("S3cur3P@ss!2024");

        ProductionConfigValidator validator = new ProductionConfigValidator(env);
        assertDoesNotThrow(() -> validator.run(null));
    }
}