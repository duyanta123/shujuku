package com.labcourse.service;

import com.labcourse.entity.Lab;
import com.labcourse.repository.LabRepository;
import com.labcourse.service.impl.LabServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * LabServiceImpl 单元测试 — 覆盖近期修改的 partial-update 与 college 字段支持（原本零覆盖）
 *
 * 风险行为覆盖：
 * - updateById: 部分字段更新保留未传入字段（college/labName/location/capacity）
 * - updateById: 不存在的 Lab 拒绝更新
 * - save: 基础保存路径
 * - removeById: 基础删除路径
 * - list: 全量查询
 */
@SuppressWarnings("null")
class LabServiceImplTest {

    private LabServiceImpl service;
    private LabRepository labRepository;

    @BeforeEach
    void setUp() {
        service = new LabServiceImpl();
        labRepository = mock(LabRepository.class);
        injectField(service, "labRepository", labRepository);
    }

    // ================================================================
    // save
    // ================================================================

    @Test
    @DisplayName("save: 应成功保存实验室")
    void save_ShouldSucceed() {
        Lab lab = new Lab();
        lab.setLabName("501实验室");
        lab.setLocation("教学楼A-501");
        lab.setCapacity(40);

        boolean result = service.save(lab);
        assertTrue(result);
        verify(labRepository).save(lab);
    }

    // ================================================================
    // list
    // ================================================================

    @Test
    @DisplayName("list: 应返回所有实验室")
    void list_ShouldReturnAll() {
        when(labRepository.findAll()).thenReturn(Collections.emptyList());
        var result = service.list();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(labRepository).findAll();
    }

    // ================================================================
    // updateById — 部分字段更新（核心变更：从全量 save 改为 partial update）
    // ================================================================

    @Test
    @DisplayName("updateById: 部分更新应保留未传入字段")
    void updateById_PartialFields_ShouldPreserveExisting() {
        Lab existing = new Lab();
        existing.setId(1L);
        existing.setLabName("501实验室");
        existing.setLocation("教学楼A-501");
        existing.setCapacity(40);
        existing.setCollegeId(1L);

        Lab update = new Lab();
        update.setId(1L);
        update.setLabName("502实验室");
        // location、capacity、college 未传入 — 应保留

        when(labRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);
        assertTrue(result);

        assertEquals("502实验室", existing.getLabName());
        assertEquals("教学楼A-501", existing.getLocation(), "未传入的 location 应保留");
        assertEquals(40, existing.getCapacity(), "未传入的 capacity 应保留");
        assertEquals(1L, existing.getCollegeId(), "未传入的 collegeId 应保留");
        verify(labRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: 更新 college 字段应生效")
    void updateById_CollegeField_ShouldUpdate() {
        Lab existing = new Lab();
        existing.setId(1L);
        existing.setLabName("501实验室");
        existing.setCollegeId(1L);

        Lab update = new Lab();
        update.setId(1L);
        update.setCollegeId(1L);

        when(labRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);
        assertTrue(result);
        assertEquals(1L, existing.getCollegeId());
        assertEquals("501实验室", existing.getLabName(), "未传入的 labName 应保留");
        verify(labRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: 更新 location 字段应生效")
    void updateById_Location_ShouldUpdate() {
        Lab existing = new Lab();
        existing.setId(1L);
        existing.setLabName("501实验室");
        existing.setLocation("旧位置");

        Lab update = new Lab();
        update.setId(1L);
        update.setLocation("新位置");

        when(labRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);
        assertTrue(result);
        assertEquals("新位置", existing.getLocation());
        verify(labRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: 更新 capacity 字段应生效")
    void updateById_Capacity_ShouldUpdate() {
        Lab existing = new Lab();
        existing.setId(1L);
        existing.setLabName("501实验室");
        existing.setCapacity(30);

        Lab update = new Lab();
        update.setId(1L);
        update.setCapacity(50);

        when(labRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);
        assertTrue(result);
        assertEquals(50, existing.getCapacity());
        verify(labRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: null 字段不应覆盖已有值")
    void updateById_NullFields_ShouldNotOverwrite() {
        Lab existing = new Lab();
        existing.setId(1L);
        existing.setLabName("501实验室");
        existing.setLocation("教学楼A-501");
        existing.setCapacity(40);
        existing.setCollegeId(1L);

        Lab update = new Lab();
        update.setId(1L);
        update.setLabName(null);
        update.setLocation(null);
        update.setCapacity(null);
        update.setCollegeId(null);

        when(labRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);
        assertTrue(result);

        assertEquals("501实验室", existing.getLabName(), "null 不应覆盖 labName");
        assertEquals("教学楼A-501", existing.getLocation(), "null 不应覆盖 location");
        assertEquals(40, existing.getCapacity(), "null 不应覆盖 capacity");
        assertEquals(1L, existing.getCollegeId(), "null 不应覆盖 collegeId");
        verify(labRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: 不存在的实验室应返回 false")
    void updateById_NonExistent_ShouldReturnFalse() {
        Lab update = new Lab();
        update.setId(999L);
        update.setLabName("不存在");

        when(labRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = service.updateById(update);
        assertFalse(result);
        verify(labRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateById: 同时更新多个字段应全部生效")
    void updateById_MultipleFields_ShouldAllUpdate() {
        Lab existing = new Lab();
        existing.setId(1L);
        existing.setLabName("旧名称");
        existing.setLocation("旧位置");
        existing.setCapacity(20);
        existing.setCollegeId(1L);

        Lab update = new Lab();
        update.setId(1L);
        update.setLabName("新名称");
        update.setLocation("新位置");
        update.setCapacity(60);
        update.setCollegeId(1L);

        when(labRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);
        assertTrue(result);

        assertEquals("新名称", existing.getLabName());
        assertEquals("新位置", existing.getLocation());
        assertEquals(60, existing.getCapacity());
        assertEquals(1L, existing.getCollegeId());
        verify(labRepository).save(existing);
    }

    // ================================================================
    // removeById
    // ================================================================

    @Test
    @DisplayName("removeById: 应成功删除实验室")
    void removeById_ShouldSucceed() {
        boolean result = service.removeById(1L);
        assertTrue(result);
        verify(labRepository).deleteById(1L);
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