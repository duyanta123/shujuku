package com.labcourse.controller;

import com.labcourse.entity.College;
import com.labcourse.service.CollegeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CollegeController 单元测试 — 覆盖学院管理 HTTP 入口层（原本零覆盖）
 *
 * 风险行为覆盖：
 * - list: 分页查询、筛选参数绑定、响应格式标准化
 * - add: 成功添加、名称重复拒绝、请求体参数绑定
 * - update: 成功更新、失败响应
 * - delete: 软删除成功、学院不存在
 */
@SuppressWarnings("null")
class CollegeControllerTest {

    private CollegeController controller;
    private CollegeService collegeService;

    @BeforeEach
    void setUp() {
        controller = new CollegeController();
        collegeService = mock(CollegeService.class);
        injectField(controller, "collegeService", collegeService);
    }

    // ================================================================
    // list — 分页查询 + 响应格式
    // ================================================================

    @Test
    @DisplayName("list: 无筛选条件时应返回分页结果")
    void list_NoFilters_ShouldReturnPaginatedResult() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("content", List.of());
        serviceResult.put("totalElements", 0L);
        serviceResult.put("totalPages", 0);
        serviceResult.put("currentPage", 0);
        when(collegeService.list(null, "ACTIVE", 0, 20, "id", "asc")).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = controller.list(null, "ACTIVE", 0, 20, "id", "asc");

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertNotNull(body.get("data"));
    }

    @Test
    @DisplayName("list: 按名称筛选时应传递参数")
    void list_ByNameFilter_ShouldPassParameter() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("content", List.of());
        when(collegeService.list("计算机", "ACTIVE", 0, 20, "id", "asc")).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = controller.list("计算机", "ACTIVE", 0, 20, "id", "asc");

        assertEquals(200, response.getStatusCode().value());
        verify(collegeService).list("计算机", "ACTIVE", 0, 20, "id", "asc");
    }

    @Test
    @DisplayName("list: 按状态筛选 INACTIVE 时应传递参数")
    void list_ByInactiveStatus_ShouldPassParameter() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("content", List.of());
        when(collegeService.list(null, "INACTIVE", 0, 20, "id", "asc")).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = controller.list(null, "INACTIVE", 0, 20, "id", "asc");

        assertEquals(200, response.getStatusCode().value());
        verify(collegeService).list(null, "INACTIVE", 0, 20, "id", "asc");
    }

    // ================================================================
    // add — 添加学院
    // ================================================================

    @Test
    @DisplayName("add: 成功添加时应返回 success=true")
    void add_Success_ShouldReturnSuccess() {
        College college = new College();
        college.setName("新学院");
        college.setStatus("ACTIVE");

        when(collegeService.save(college)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.add(college);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("添加成功", body.get("message"));
        verify(collegeService).save(college);
    }

    @Test
    @DisplayName("add: 名称重复时应返回 success=false")
    void add_DuplicateName_ShouldReturnFailure() {
        College college = new College();
        college.setName("已存在的学院");

        when(collegeService.save(college)).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.add(college);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertTrue(((String) body.get("message")).contains("重复"));
        verify(collegeService).save(college);
    }

    // ================================================================
    // update — 更新学院
    // ================================================================

    @Test
    @DisplayName("update: 成功更新时应返回 success=true")
    void update_Success_ShouldReturnSuccess() {
        College college = new College();
        college.setId(1L);
        college.setName("更新后名称");

        when(collegeService.update(college)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.update(college);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("更新成功", body.get("message"));
        verify(collegeService).update(college);
    }

    @Test
    @DisplayName("update: 更新失败时应返回 success=false")
    void update_Failure_ShouldReturnFailure() {
        College college = new College();
        college.setId(999L);
        college.setName("不存在的学院");

        when(collegeService.update(college)).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.update(college);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("更新失败", body.get("message"));
        verify(collegeService).update(college);
    }

    // ================================================================
    // delete — 软删除
    // ================================================================

    @Test
    @DisplayName("delete: 成功软删除时应返回 success=true")
    void delete_Success_ShouldReturnSuccess() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("success", true);
        serviceResult.put("message", "学院已停用");
        when(collegeService.delete(1L)).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = controller.delete(1L);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("学院已停用", body.get("message"));
        verify(collegeService).delete(1L);
    }

    @Test
    @DisplayName("delete: 学院不存在时应返回 success=false")
    void delete_NonExistent_ShouldReturnFailure() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("success", false);
        serviceResult.put("message", "学院不存在");
        when(collegeService.delete(999L)).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = controller.delete(999L);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("学院不存在", body.get("message"));
        verify(collegeService).delete(999L);
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