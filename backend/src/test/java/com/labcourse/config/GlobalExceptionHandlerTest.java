package com.labcourse.config;

import com.labcourse.exception.AccountLockedException;
import com.labcourse.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler 单元测试 — 覆盖全局异常处理的所有分支（原本零覆盖）
 *
 * 风险行为覆盖：
 * - MethodArgumentTypeMismatchException: 类型转换异常 → 200 + 空数据（容错处理）
 * - MethodArgumentNotValidException: 参数校验失败 → 400 + 字段级错误详情
 * - IllegalArgumentException: 非法参数 → 400 + 错误消息
 * - AccountLockedException: 账号锁定 → 423 + 锁定信息
 * - Exception: 通用异常 → 500 + 固定消息（生产环境不泄露内部细节）
 * - 边界情况: null 消息、多字段校验错误
 */
@SuppressWarnings("null")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ================================================================
    // MethodArgumentTypeMismatchException
    // ================================================================

    @Test
    @DisplayName("类型转换异常应返回 200 + 空数据（容错降级）")
    void handleTypeMismatch_ShouldReturn400WithError() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "123abc", Integer.class, "id", null, null
        );

        ResponseEntity<Map<String, Object>> response = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertTrue(body.get("message").toString().contains("id"));
    }

    // ================================================================
    // MethodArgumentNotValidException — 参数校验失败
    // ================================================================

    @Test
    @DisplayName("参数校验失败应返回 400 + 字段级错误详情")
    void handleValidationExceptions_ShouldReturn400WithFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                new Object(), "testObject"
        );
        bindingResult.addError(new FieldError("testObject", "name", "名称不能为空"));
        bindingResult.addError(new FieldError("testObject", "email", "邮箱格式不正确"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("参数验证失败", body.get("message"));

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertNotNull(errors);
        assertEquals(2, errors.size());
        assertEquals("名称不能为空", errors.get("name"));
        assertEquals("邮箱格式不正确", errors.get("email"));
    }

    @Test
    @DisplayName("单字段校验失败应返回正确的错误键值")
    void handleValidationExceptions_SingleField_ShouldReturnCorrectError() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                new Object(), "testObject"
        );
        bindingResult.addError(new FieldError("testObject", "password", "密码长度不足"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertEquals(1, errors.size());
        assertEquals("密码长度不足", errors.get("password"));
    }

    // ================================================================
    // IllegalArgumentException
    // ================================================================

    @Test
    @DisplayName("非法参数异常应返回 400 + 异常消息")
    void handleIllegalArgumentException_WithMessage_ShouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("学院ID不能为空");

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("学院ID不能为空", body.get("message"));
    }

    @Test
    @DisplayName("非法参数异常消息为 null 时应返回默认消息")
    void handleIllegalArgumentException_NullMessage_ShouldReturnDefaultMessage() {
        IllegalArgumentException ex = new IllegalArgumentException();

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("非法参数", body.get("message"));
    }

    // ================================================================
    // AccountLockedException
    // ================================================================

    @Test
    @DisplayName("账号锁定异常应返回 423 + 锁定信息")
    void handleAccountLockedException_ShouldReturn423() {
        AccountLockedException ex = new AccountLockedException(
                "账号已被锁定，请30分钟后重试", 30L
        );

        ResponseEntity<Map<String, Object>> response = handler.handleAccountLockedException(ex);

        assertEquals(423, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("账号已被锁定，请30分钟后重试", body.get("message"));
        assertEquals(true, body.get("locked"));
        assertEquals(30L, body.get("remainingMinutes"));
    }

    @Test
    @DisplayName("账号锁定异常应包含 remainingMinutes 字段")
    void handleAccountLockedException_ShouldIncludeRemainingMinutes() {
        AccountLockedException ex = new AccountLockedException("锁定", 5L);

        ResponseEntity<Map<String, Object>> response = handler.handleAccountLockedException(ex);

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(5L, body.get("remainingMinutes"));
        assertEquals(true, body.get("locked"));
    }

    // ================================================================
    // BusinessException — 新增异常类型（commit 4274e4f）
    // ================================================================

    @Test
    @DisplayName("BusinessException: 应返回异常中携带的 HTTP 状态码、code 和 message")
    void handleBusinessException_ShouldReturnHttpStatusAndCode() {
        BusinessException ex = new BusinessException("COURSE_HAS_SELECTIONS",
                "课程已有选课记录，无法删除", HttpStatus.CONFLICT);

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("COURSE_HAS_SELECTIONS", body.get("code"));
        assertEquals("课程已有选课记录，无法删除", body.get("message"));
    }

    @Test
    @DisplayName("BusinessException: BAD_REQUEST 状态码应正确传递")
    void handleBusinessException_BadRequest_ShouldReturn400() {
        BusinessException ex = new BusinessException("INVALID_COURSE_TYPE",
                "courseType must be REQUIRED or ELECTIVE", HttpStatus.BAD_REQUEST);

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_COURSE_TYPE", response.getBody().get("code"));
    }

    @Test
    @DisplayName("BusinessException: FORBIDDEN 状态码应正确传递")
    void handleBusinessException_Forbidden_ShouldReturn403() {
        BusinessException ex = new BusinessException("FORBIDDEN",
                "无权查看此课程的成绩", HttpStatus.FORBIDDEN);

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("FORBIDDEN", response.getBody().get("code"));
    }

    // ================================================================
    // Exception — 通用异常
    // ================================================================

    @Test
    @DisplayName("通用异常应返回 500 + 固定消息（不泄露内部细节）")
    void handleGeneralException_ShouldReturn500WithFixedMessage() {
        Exception ex = new Exception("数据库连接失败：ORA-12541: TNS:no listener");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneralException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("服务器内部错误", body.get("message"));
        // 关键：不应包含原始异常消息中的细节
        assertNotEquals("数据库连接失败：ORA-12541: TNS:no listener", body.get("message"));
    }

    @Test
    @DisplayName("NullPointerException 也应返回 500 + 固定消息")
    void handleGeneralException_NullPointer_ShouldReturn500() {
        Exception ex = new NullPointerException("Cannot invoke method on null");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneralException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("服务器内部错误", body.get("message"));
    }
}
