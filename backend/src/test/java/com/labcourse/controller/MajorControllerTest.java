package com.labcourse.controller;

import com.labcourse.entity.Major;
import com.labcourse.service.MajorService;
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
 * MajorController 单元测试 — 覆盖专业管理 HTTP 入口层（原本零覆盖）
 *
 * 风险行为覆盖：
 * - list: 多条件筛选（name + collegeId + status）、分页、响应格式
 * - listByCollege: 按学院ID查询专业（前端级联下拉使用）
 * - add: 成功添加、同名拒绝
 * - update: 成功更新、失败响应
 * - delete: 软删除（含关联学生检查）、专业不存在
 */
@SuppressWarnings("null")
class MajorControllerTest {

    private MajorController controller;
    private MajorService majorService;

    @BeforeEach
    void setUp() {
        controller = new MajorController();
        majorService = mock(MajorService.class);
        injectField(controller, "majorService", majorService);
    }

    // ================================================================
    // list — 多条件筛选 + 分页
    // ================================================================

    @Test
    @DisplayName("list: 无筛选条件时应返回分页结果")
    void list_NoFilters_ShouldReturnPaginatedResult() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("content", List.of());
        serviceResult.put("totalElements", 0L);
        when(majorService.list(null, null, "ACTIVE", 0, 20, "id", "asc")).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = controller.list(null, null, "ACTIVE", 0, 20, "id", "asc");

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertNotNull(body.get("data"));
    }

    @Test
    @DisplayName("list: 按学院ID筛选时应传递参数")
    void list_ByCollegeId_ShouldPassParameter() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("content", List.of());
        when(majorService.list(null, 1L, "ACTIVE", 0, 20, "id", "asc")).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = controller.list(null, 1L, "ACTIVE", 0, 20, "id", "asc");

        assertEquals(200, response.getStatusCode().value());
        verify(majorService).list(null, 1L, "ACTIVE", 0, 20, "id", "asc");
    }

    @Test
    @DisplayName("list: 按名称+学院+状态组合筛选")
    void list_ByNameAndCollegeIdAndStatus_ShouldPassAllParams() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("content", List.of());
        when(majorService.list("计算机", 1L, "INACTIVE", 0, 20, "id", "asc")).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = controller.list("计算机", 1L, "INACTIVE", 0, 20, "id", "asc");

        assertEquals(200, response.getStatusCode().value());
        verify(majorService).list("计算机", 1L, "INACTIVE", 0, 20, "id", "asc");
    }

    // ================================================================
    // listByCollege — 级联下拉数据源
    // ================================================================

    @Test
    @DisplayName("listByCollege: 应返回指定学院的 ACTIVE 专业列表")
    void listByCollege_ShouldReturnActiveMajors() {
        Major m1 = new Major(); m1.setId(1L); m1.setName("计算机科学");
        Major m2 = new Major(); m2.setId(2L); m2.setName("软件工程");
        when(majorService.listByCollegeId(1L)).thenReturn(List.of(m1, m2));

        ResponseEntity<Map<String, Object>> response = controller.listByCollege(1L);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        verify(majorService).listByCollegeId(1L);
    }

    @Test
    @DisplayName("listByCollege: 学院无专业时应返回空列表")
    void listByCollege_NoMajors_ShouldReturnEmptyList() {
        when(majorService.listByCollegeId(999L)).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.listByCollege(999L);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        verify(majorService).listByCollegeId(999L);
    }

    // ================================================================
    // add — 添加专业
    // ================================================================

    @Test
    @DisplayName("add: 成功添加时应返回 success=true")
    void add_Success_ShouldReturnSuccess() {
        Major major = new Major();
        major.setName("人工智能");
        major.setCollegeId(1L);

        when(majorService.save(major)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.add(major);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("添加成功", body.get("message"));
        verify(majorService).save(major);
    }

    @Test
    @DisplayName("add: 同学院下名称重复时应返回失败")
    void add_DuplicateName_SameCollege_ShouldReturnFailure() {
        Major major = new Major();
        major.setName("计算机科学");
        major.setCollegeId(1L);

        when(majorService.save(major)).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.add(major);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertTrue(((String) body.get("message")).contains("同名"));
        verify(majorService).save(major);
    }

    // ================================================================
    // update — 更新专业
    // ================================================================

    @Test
    @DisplayName("update: 成功更新时应返回 success=true")
    void update_Success_ShouldReturnSuccess() {
        Major major = new Major();
        major.setId(1L);
        major.setName("更新后专业名");

        when(majorService.update(major)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.update(major);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("更新成功", body.get("message"));
        verify(majorService).update(major);
    }

    @Test
    @DisplayName("update: 更新失败时应返回 success=false")
    void update_Failure_ShouldReturnFailure() {
        Major major = new Major();
        major.setId(999L);
        major.setName("不存在");

        when(majorService.update(major)).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.update(major);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("更新失败", body.get("message"));
        verify(majorService).update(major);
    }

    // ================================================================
    // delete — 软删除 + 关联学生检查
    // ================================================================

    @Test
    @DisplayName("delete: 无关联学生时软删除应成功")
    void delete_NoStudents_ShouldSoftDelete() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("success", true);
        serviceResult.put("message", "专业已停用");
        when(majorService.delete(1L)).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = controller.delete(1L);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("专业已停用", body.get("message"));
        verify(majorService).delete(1L);
    }

    @Test
    @DisplayName("delete: 有关联学生时应拒绝删除")
    void delete_WithStudents_ShouldReturnFailure() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("success", false);
        serviceResult.put("message", "该专业下存在 10 名学生，无法删除");
        when(majorService.delete(1L)).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = controller.delete(1L);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertTrue(((String) body.get("message")).contains("名学生"));
        verify(majorService).delete(1L);
    }

    @Test
    @DisplayName("delete: 专业不存在时应返回失败")
    void delete_NonExistent_ShouldReturnFailure() {
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("success", false);
        serviceResult.put("message", "专业不存在");
        when(majorService.delete(999L)).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = controller.delete(999L);

        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("专业不存在", body.get("message"));
        verify(majorService).delete(999L);
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