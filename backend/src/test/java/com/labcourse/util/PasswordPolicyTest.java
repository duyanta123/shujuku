package com.labcourse.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordPolicy 单元测试 — 覆盖密码校验、复杂度检查与临时密码生成
 *
 * 风险行为覆盖：
 * - requireValid: 合法密码通过 / 不合法密码抛出 IllegalArgumentException
 * - getValidationError: null/空/空格/长度不足/长度过长/复杂度不足 各类错误码
 * - generateTemporaryPassword: 长度、字符类型、随机性
 */
@DisplayName("PasswordPolicy 密码策略工具")
class PasswordPolicyTest {

    // ================================================================
    // requireValid — 异常抛出验证
    // ================================================================

    @Test
    @DisplayName("requireValid: 合法密码不抛异常")
    void requireValid_ValidPassword_ShouldNotThrow() {
        assertDoesNotThrow(() -> PasswordPolicy.requireValid("Abc123!@"));
    }

    @Test
    @DisplayName("requireValid: 空密码抛出 IllegalArgumentException")
    void requireValid_NullPassword_ShouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.requireValid(null));
        assertEquals("密码不能为空", ex.getMessage());
    }

    @Test
    @DisplayName("requireValid: 复杂度不足密码抛出 IllegalArgumentException")
    void requireValid_WeakPassword_ShouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.requireValid("abcdefgh"));
        assertTrue(ex.getMessage().contains("密码复杂度不足"));
    }

    // ================================================================
    // getValidationError — 各类错误码
    // ================================================================

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("getValidationError: null 或空字符串返回'密码不能为空'")
    void getValidationError_NullOrEmpty_ShouldReturnEmptyError(String password) {
        assertEquals("密码不能为空", PasswordPolicy.getValidationError(password));
    }

    @Test
    @DisplayName("getValidationError: 包含空格返回空格错误")
    void getValidationError_ContainsSpace_ShouldReturnSpaceError() {
        assertEquals("密码中不能包含空格", PasswordPolicy.getValidationError("abc 123!@"));
    }

    @Test
    @DisplayName("getValidationError: 长度不足8位返回长度不足错误")
    void getValidationError_TooShort_ShouldReturnLengthError() {
        assertEquals("密码长度不足，至少需要8个字符", PasswordPolicy.getValidationError("Ab1!"));
    }

    @Test
    @DisplayName("getValidationError: 超过20位返回长度过长错误")
    void getValidationError_TooLong_ShouldReturnLengthError() {
        assertEquals("密码长度过长，最多20个字符", PasswordPolicy.getValidationError("A".repeat(21) + "b1!"));
    }

    @Test
    @DisplayName("getValidationError: 恰好8位合法密码返回 null")
    void getValidationError_Exactly8Chars_ShouldReturnNull() {
        assertNull(PasswordPolicy.getValidationError("Abc123!@"));
    }

    @Test
    @DisplayName("getValidationError: 恰好20位合法密码返回 null")
    void getValidationError_Exactly20Chars_ShouldReturnNull() {
        assertNull(PasswordPolicy.getValidationError("Abc123!@Xyz789#%^Kl"));
    }

    // ================================================================
    // getValidationError — 复杂度不足（只含1种或2种字符）
    // ================================================================

    @Test
    @DisplayName("getValidationError: 仅小写字母 — 复杂度不足，缺少大写字母、数字、特殊符号")
    void getValidationError_OnlyLowercase_ShouldReturnMissingError() {
        String error = PasswordPolicy.getValidationError("abcdefghij");
        assertNotNull(error);
        assertTrue(error.contains("密码复杂度不足"));
        assertTrue(error.contains("大写字母"));
        assertTrue(error.contains("数字"));
        assertTrue(error.contains("特殊符号"));
    }

    @Test
    @DisplayName("getValidationError: 仅大写字母 — 复杂度不足，缺少小写字母、数字、特殊符号")
    void getValidationError_OnlyUppercase_ShouldReturnMissingError() {
        String error = PasswordPolicy.getValidationError("ABCDEFGHIJ");
        assertNotNull(error);
        assertTrue(error.contains("小写字母"));
    }

    @Test
    @DisplayName("getValidationError: 仅数字 — 复杂度不足")
    void getValidationError_OnlyDigits_ShouldReturnMissingError() {
        String error = PasswordPolicy.getValidationError("1234567890");
        assertNotNull(error);
        assertTrue(error.contains("密码复杂度不足"));
        assertTrue(error.contains("小写字母"));
        assertTrue(error.contains("大写字母"));
        assertTrue(error.contains("特殊符号"));
    }

    @Test
    @DisplayName("getValidationError: 仅特殊符号 — 复杂度不足")
    void getValidationError_OnlySpecial_ShouldReturnMissingError() {
        String error = PasswordPolicy.getValidationError("!@#$%^&*()");
        assertNotNull(error);
        assertTrue(error.contains("密码复杂度不足"));
        assertTrue(error.contains("小写字母"));
        assertTrue(error.contains("大写字母"));
        assertTrue(error.contains("数字"));
    }

    @Test
    @DisplayName("getValidationError: 小写+大写（2种）— 复杂度不足，缺少数字和特殊符号")
    void getValidationError_LowerUpper_ShouldReturnMissingError() {
        String error = PasswordPolicy.getValidationError("AbcDefGhIj");
        assertNotNull(error);
        assertTrue(error.contains("密码复杂度不足"));
        assertTrue(error.contains("数字"));
        assertTrue(error.contains("特殊符号"));
    }

    @Test
    @DisplayName("getValidationError: 小写+数字（2种）— 复杂度不足")
    void getValidationError_LowerDigit_ShouldReturnMissingError() {
        String error = PasswordPolicy.getValidationError("abcdef1234");
        assertNotNull(error);
        assertTrue(error.contains("密码复杂度不足"));
        assertTrue(error.contains("大写字母"));
        assertTrue(error.contains("特殊符号"));
    }

    @Test
    @DisplayName("getValidationError: 包含中文（特殊字符类别）— 视为包含特殊符号，可能满足3种")
    void getValidationError_WithChinese_MayPassComplexity() {
        // 中文属于非字母数字字符，匹配 [^a-zA-Z0-9]，算作特殊符号
        // "中文Abc123" 包含小写+大写+数字+特殊符号(中文) = 4种，满足要求
        assertNull(PasswordPolicy.getValidationError("中文Abc123"));
    }

    // ================================================================
    // generateTemporaryPassword — 临时密码生成
    // ================================================================

    @Test
    @DisplayName("generateTemporaryPassword: 生成密码长度为12")
    void generateTemporaryPassword_Length_ShouldBe12() {
        String password = PasswordPolicy.generateTemporaryPassword();
        assertEquals(12, password.length());
    }

    @Test
    @DisplayName("generateTemporaryPassword: 生成的密码至少包含小写、大写、数字、特殊符号各一种")
    void generateTemporaryPassword_ShouldContainAllCharTypes() {
        String password = PasswordPolicy.generateTemporaryPassword();
        assertTrue(password.matches(".*[a-z].*"), "应包含小写字母");
        assertTrue(password.matches(".*[A-Z].*"), "应包含大写字母");
        assertTrue(password.matches(".*[0-9].*"), "应包含数字");
        assertTrue(password.matches(".*[^a-zA-Z0-9].*"), "应包含特殊符号");
    }

    @Test
    @DisplayName("generateTemporaryPassword: 生成的密码应通过自身策略校验")
    void generateTemporaryPassword_ShouldPassSelfValidation() {
        for (int i = 0; i < 10; i++) {
            String password = PasswordPolicy.generateTemporaryPassword();
            assertNull(PasswordPolicy.getValidationError(password),
                    "生成的密码应通过自身校验: " + password);
        }
    }

    @Test
    @DisplayName("generateTemporaryPassword: 连续生成应有不同结果（随机性验证）")
    void generateTemporaryPassword_ShouldBeRandom() {
        String p1 = PasswordPolicy.generateTemporaryPassword();
        String p2 = PasswordPolicy.generateTemporaryPassword();
        String p3 = PasswordPolicy.generateTemporaryPassword();
        // 连续3次生成结果不应完全相同
        assertFalse(p1.equals(p2) && p2.equals(p3),
                "连续3次生成的密码不应完全相同");
    }

    @Test
    @DisplayName("generateTemporaryPassword: 不包含空格")
    void generateTemporaryPassword_ShouldNotContainSpace() {
        for (int i = 0; i < 20; i++) {
            String password = PasswordPolicy.generateTemporaryPassword();
            assertFalse(password.contains(" "), "密码不应包含空格");
        }
    }
}