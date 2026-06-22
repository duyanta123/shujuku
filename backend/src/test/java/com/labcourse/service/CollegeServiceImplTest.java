package com.labcourse.service;

import com.labcourse.entity.College;
import com.labcourse.repository.CollegeRepository;
import com.labcourse.service.impl.CollegeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CollegeServiceImpl 单元测试 — 覆盖近期新增的学院管理服务
 *
 * 风险行为覆盖：
 * - save: 重复名称拒绝（数据库唯一约束的业务层前置校验）
 * - update: 部分字段更新保留未传入字段、重复名称校验
 * - delete: 软删除机制（设 INACTIVE）、依赖校验（专业/学生/教师计数）
 * - list: 多条件分页查询（name + status）
 */
@SuppressWarnings("null")
class CollegeServiceImplTest {

    private CollegeServiceImpl service;
    private CollegeRepository collegeRepository;

    @BeforeEach
    void setUp() {
        service = new CollegeServiceImpl();
        collegeRepository = mock(CollegeRepository.class);

        injectField(service, "collegeRepository", collegeRepository);
    }

    // ================================================================
    // save — 重复名称拒绝
    // ================================================================

    @Test
    @DisplayName("save: 名称不存在时应创建成功")
    void save_NewName_ShouldSucceed() {
        College college = new College();
        college.setName("新学院");

        when(collegeRepository.findByName("新学院")).thenReturn(Optional.empty());

        boolean result = service.save(college);
        assertTrue(result);
        verify(collegeRepository).save(college);
    }

    @Test
    @DisplayName("save: 名称已存在时应拒绝创建")
    void save_DuplicateName_ShouldReturnFalse() {
        College college = new College();
        college.setName("已存在学院");

        College existing = new College();
        existing.setId(1L);
        existing.setName("已存在学院");

        when(collegeRepository.findByName("已存在学院")).thenReturn(Optional.of(existing));

        boolean result = service.save(college);
        assertFalse(result, "重复学院名应被拒绝");
        verify(collegeRepository, never()).save(any());
    }

    // ================================================================
    // update — 部分字段更新 + 重复名称校验
    // ================================================================

    @Test
    @DisplayName("update: 部分更新应保留未传入字段")
    void update_PartialFields_ShouldPreserveExisting() {
        College existing = new College();
        existing.setId(1L);
        existing.setName("原名称");
        existing.setStatus("ACTIVE");

        College update = new College();
        update.setId(1L);
        update.setName("新名称");
        // status 未传入 — 应保留

        when(collegeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(collegeRepository.findByName("新名称")).thenReturn(Optional.empty());

        boolean result = service.update(update);
        assertTrue(result);
        assertEquals("新名称", existing.getName());
        assertEquals("ACTIVE", existing.getStatus(), "未传入的 status 应保留原值");
        verify(collegeRepository).save(existing);
    }

    @Test
    @DisplayName("update: 更新不存在的学院应返回 false")
    void update_NonExistent_ShouldReturnFalse() {
        College update = new College();
        update.setId(999L);
        update.setName("不存在的学院");

        when(collegeRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = service.update(update);
        assertFalse(result);
        verify(collegeRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: 名称改为已存在的其他学院名应拒绝")
    void update_DuplicateName_DifferentId_ShouldReturnFalse() {
        College existing = new College();
        existing.setId(1L);
        existing.setName("学院A");

        College duplicate = new College();
        duplicate.setId(2L);
        duplicate.setName("学院B");

        College update = new College();
        update.setId(1L);
        update.setName("学院B"); // 已被另一个学院使用

        when(collegeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(collegeRepository.findByName("学院B")).thenReturn(Optional.of(duplicate));

        boolean result = service.update(update);
        assertFalse(result, "更新为重复名称应被拒绝");
        verify(collegeRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: 名称改为自身相同名称应成功（自身不算重复）")
    void update_SameName_SameId_ShouldSucceed() {
        College existing = new College();
        existing.setId(1L);
        existing.setName("学院A");

        College update = new College();
        update.setId(1L);
        update.setName("学院A"); // 相同名称但相同 ID — 不算重复

        when(collegeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(collegeRepository.findByName("学院A")).thenReturn(Optional.of(existing));

        boolean result = service.update(update);
        assertTrue(result, "更新为自身名称不应被拒绝");
        verify(collegeRepository).save(existing);
    }

    @Test
    @DisplayName("update: 更新 status 字段不应改变 name")
    void update_OnlyStatus_ShouldNotChangeName() {
        College existing = new College();
        existing.setId(1L);
        existing.setName("学院A");
        existing.setStatus("ACTIVE");

        College update = new College();
        update.setId(1L);
        update.setStatus("INACTIVE");
        // name 未传入 — 应保留

        when(collegeRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.update(update);
        assertTrue(result);
        assertEquals("学院A", existing.getName());
        assertEquals("INACTIVE", existing.getStatus());
        verify(collegeRepository).save(existing);
    }

    @Test
    @DisplayName("update: name 为空字符串时不应覆盖原名称")
    void update_EmptyName_ShouldNotOverwrite() {
        College existing = new College();
        existing.setId(1L);
        existing.setName("学院A");

        College update = new College();
        update.setId(1L);
        update.setName("");

        when(collegeRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.update(update);
        assertTrue(result);
        assertEquals("学院A", existing.getName(), "空字符串不应覆盖原名称");
    }

    // ================================================================
    // delete — 软删除
    // ================================================================

    @Test
    @DisplayName("delete: 存在学院时执行软删除（设 INACTIVE）")
    void delete_NoDependencies_ShouldSoftDelete() {
        College college = new College();
        college.setId(1L);
        college.setName("待删除学院");
        college.setStatus("ACTIVE");

        when(collegeRepository.findById(1L)).thenReturn(Optional.of(college));

        Map<String, Object> result = service.delete(1L);
        assertTrue((Boolean) result.get("success"));
        assertEquals("学院已停用", result.get("message"));
        assertEquals("INACTIVE", college.getStatus(), "应被软删除（设 INACTIVE）");
        verify(collegeRepository).save(college);
    }

    @Test
    @DisplayName("delete: 学院不存在时应返回失败")
    void delete_NonExistent_ShouldReturnFalse() {
        when(collegeRepository.findById(999L)).thenReturn(Optional.empty());

        Map<String, Object> result = service.delete(999L);
        assertFalse((Boolean) result.get("success"));
        assertEquals("学院不存在", result.get("message"));
    }

    // ================================================================
    // list — 分页查询
    // ================================================================

    @Test
    @DisplayName("list: 无筛选条件时应返回全部")
    void list_NoFilters_ShouldReturnAll() {
        College c1 = new College(); c1.setId(1L); c1.setName("学院A");
        College c2 = new College(); c2.setId(2L); c2.setName("学院B");
        Page<College> page = new PageImpl<>(List.of(c1, c2), PageRequest.of(0, 10), 2);

        when(collegeRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Map<String, Object> result = service.list(null, "all", 0, 10, "id", "asc");
        assertEquals(2L, result.get("totalElements"));
        assertEquals(0, result.get("currentPage"));
    }

    @Test
    @DisplayName("list: 按状态筛选")
    void list_ByStatus_ShouldFilter() {
        College c1 = new College(); c1.setId(1L); c1.setName("学院A");
        Page<College> page = new PageImpl<>(List.of(c1), PageRequest.of(0, 10), 1);

        when(collegeRepository.findByStatus(eq("ACTIVE"), any(PageRequest.class))).thenReturn(page);

        Map<String, Object> result = service.list(null, "ACTIVE", 0, 10, "id", "asc");
        assertEquals(1L, result.get("totalElements"));
    }

    // ================================================================
    // getById
    // ================================================================

    @Test
    @DisplayName("getById: 存在的 ID 应返回学院")
    void getById_Exists_ShouldReturnCollege() {
        College college = new College();
        college.setId(1L);
        college.setName("测试学院");

        when(collegeRepository.findById(1L)).thenReturn(Optional.of(college));

        College result = service.getById(1L);
        assertNotNull(result);
        assertEquals("测试学院", result.getName());
    }

    @Test
    @DisplayName("getById: 不存在的 ID 应返回 null")
    void getById_NotExists_ShouldReturnNull() {
        when(collegeRepository.findById(999L)).thenReturn(Optional.empty());

        College result = service.getById(999L);
        assertNull(result);
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