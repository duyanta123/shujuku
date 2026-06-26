package com.labcourse.service;

import com.labcourse.entity.College;
import com.labcourse.entity.Major;
import com.labcourse.repository.CollegeRepository;
import com.labcourse.repository.MajorRequiredCourseRepository;
import com.labcourse.repository.MajorRepository;
import com.labcourse.repository.StudentRepository;
import com.labcourse.service.impl.MajorServiceImpl;
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
 * MajorServiceImpl 单元测试 — 覆盖近期新增的专业管理服务（原本零覆盖）
 *
 * 风险行为覆盖：
 * - save: 同一学院下重复名称拒绝（数据库唯一约束的业务层前置校验）
 * - update: 部分字段更新保留未传入字段、重复名称校验
 * - delete: 软删除机制（设 INACTIVE）、依赖校验（关联学生计数）
 * - list: 多条件分页查询（name + collegeId + status 组合）
 * - listByCollegeId: 仅返回 ACTIVE 状态的专业
 */
@SuppressWarnings("null")
class MajorServiceImplTest {

    private MajorServiceImpl service;
    private MajorRepository majorRepository;
    private StudentRepository studentRepository;
    private CollegeRepository collegeRepository;
    private MajorRequiredCourseRepository majorRequiredCourseRepository;

    @BeforeEach
    void setUp() {
        service = new MajorServiceImpl();
        majorRepository = mock(MajorRepository.class);
        studentRepository = mock(StudentRepository.class);
        collegeRepository = mock(CollegeRepository.class);
        majorRequiredCourseRepository = mock(MajorRequiredCourseRepository.class);

        injectField(service, "majorRepository", majorRepository);
        injectField(service, "studentRepository", studentRepository);
        injectField(service, "collegeRepository", collegeRepository);
        injectField(service, "majorRequiredCourseRepository", majorRequiredCourseRepository);

        when(collegeRepository.findById(anyLong())).thenAnswer(invocation -> {
            College college = new College();
            college.setId(invocation.getArgument(0));
            college.setStatus("ACTIVE");
            return Optional.of(college);
        });
        when(majorRequiredCourseRepository.findByMajorId(anyLong())).thenReturn(List.of());
    }

    // ================================================================
    // save — 重复名称拒绝
    // ================================================================

    @Test
    @DisplayName("save: 同一学院下名称不重复时应创建成功")
    void save_NewName_ShouldSucceed() {
        Major major = new Major();
        major.setName("人工智能");
        major.setCollegeId(1L);

        when(majorRepository.findByCollegeIdAndName(1L, "人工智能")).thenReturn(Optional.empty());

        boolean result = service.save(major);
        assertTrue(result);
        verify(majorRepository).save(major);
    }

    @Test
    @DisplayName("save: 同一学院下名称已存在时应拒绝")
    void save_DuplicateName_SameCollege_ShouldReturnFalse() {
        Major major = new Major();
        major.setName("计算机科学");
        major.setCollegeId(1L);

        Major existing = new Major();
        existing.setId(5L);
        existing.setName("计算机科学");
        existing.setCollegeId(1L);

        when(majorRepository.findByCollegeIdAndName(1L, "计算机科学")).thenReturn(Optional.of(existing));

        boolean result = service.save(major);
        assertFalse(result, "同一学院下重复专业名应被拒绝");
        verify(majorRepository, never()).save(any());
    }

    @Test
    @DisplayName("save: 不同学院下相同名称应允许")
    void save_SameName_DifferentCollege_ShouldSucceed() {
        Major major = new Major();
        major.setName("计算机科学");
        major.setCollegeId(2L);  // 不同学院

        when(majorRepository.findByCollegeIdAndName(2L, "计算机科学")).thenReturn(Optional.empty());

        boolean result = service.save(major);
        assertTrue(result, "不同学院下相同专业名应被允许");
        verify(majorRepository).save(major);
    }

    // ================================================================
    // update — 部分字段更新 + 重复名称校验
    // ================================================================

    @Test
    @DisplayName("update: 部分更新应保留未传入字段")
    void update_PartialFields_ShouldPreserveExisting() {
        Major existing = new Major();
        existing.setId(1L);
        existing.setName("原专业名");
        existing.setCollegeId(1L);
        existing.setStatus("ACTIVE");

        Major update = new Major();
        update.setId(1L);
        update.setName("新专业名");
        // collegeId、status 未传入 — 应保留

        when(majorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(majorRepository.findByCollegeIdAndName(1L, "新专业名")).thenReturn(Optional.empty());

        boolean result = service.update(update);
        assertTrue(result);
        assertEquals("新专业名", existing.getName());
        assertEquals(1L, existing.getCollegeId(), "未传入的 collegeId 应保留");
        assertEquals("ACTIVE", existing.getStatus(), "未传入的 status 应保留");
        verify(majorRepository).save(existing);
    }

    @Test
    @DisplayName("update: 更新不存在的专业应返回 false")
    void update_NonExistent_ShouldReturnFalse() {
        Major update = new Major();
        update.setId(999L);
        update.setName("不存在");

        when(majorRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = service.update(update);
        assertFalse(result);
        verify(majorRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: 名称改为同学院下已存在的其他专业名应拒绝")
    void update_DuplicateName_DifferentId_SameCollege_ShouldReturnFalse() {
        Major existing = new Major();
        existing.setId(1L);
        existing.setName("专业A");
        existing.setCollegeId(1L);

        Major duplicate = new Major();
        duplicate.setId(2L);
        duplicate.setName("专业B");
        duplicate.setCollegeId(1L);

        Major update = new Major();
        update.setId(1L);
        update.setName("专业B");  // 已被同学院的专业2使用
        update.setCollegeId(1L);   // 同学院下的重复名称检查

        when(majorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(majorRepository.findByCollegeIdAndName(1L, "专业B")).thenReturn(Optional.of(duplicate));

        boolean result = service.update(update);
        assertFalse(result, "更新为同学院重复名称应被拒绝");
        verify(majorRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: 自身同名更新应成功（自身不算重复）")
    void update_SameName_SameId_ShouldSucceed() {
        Major existing = new Major();
        existing.setId(1L);
        existing.setName("专业A");
        existing.setCollegeId(1L);

        Major update = new Major();
        update.setId(1L);
        update.setName("专业A");

        when(majorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(majorRepository.findByCollegeIdAndName(1L, "专业A")).thenReturn(Optional.of(existing));

        boolean result = service.update(update);
        assertTrue(result, "更新为自身名称不应被拒绝");
        verify(majorRepository).save(existing);
    }

    @Test
    @DisplayName("update: 空名称不应覆盖原名称")
    void update_EmptyName_ShouldNotOverwrite() {
        Major existing = new Major();
        existing.setId(1L);
        existing.setName("专业A");

        Major update = new Major();
        update.setId(1L);
        update.setName("");

        when(majorRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.update(update);
        assertTrue(result);
        assertEquals("专业A", existing.getName(), "空字符串不应覆盖");
    }

    @Test
    @DisplayName("update: 仅更新 collegeId 应保留原有名称")
    void update_OnlyCollegeId_ShouldPreserveName() {
        Major existing = new Major();
        existing.setId(1L);
        existing.setName("计算机科学");
        existing.setCollegeId(1L);

        Major update = new Major();
        update.setId(1L);
        update.setCollegeId(2L);

        when(majorRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(majorRepository.findByCollegeIdAndName(2L, "计算机科学")).thenReturn(Optional.empty());

        boolean result = service.update(update);
        assertTrue(result);
        assertEquals("计算机科学", existing.getName());
        assertEquals(2L, existing.getCollegeId());
    }

    @Test
    @DisplayName("update: 仅更新 status 应保留其他字段")
    void update_OnlyStatus_ShouldPreserveOthers() {
        Major existing = new Major();
        existing.setId(1L);
        existing.setName("专业A");
        existing.setCollegeId(1L);
        existing.setStatus("ACTIVE");

        Major update = new Major();
        update.setId(1L);
        update.setStatus("INACTIVE");

        when(majorRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.update(update);
        assertTrue(result);
        assertEquals("专业A", existing.getName());
        assertEquals(1L, existing.getCollegeId());
        assertEquals("INACTIVE", existing.getStatus());
    }

    // ================================================================
    // delete — 软删除 + 依赖校验
    // ================================================================

    @Test
    @DisplayName("delete: 无关联学生时执行软删除")
    void delete_NoDependencies_ShouldSoftDelete() {
        Major major = new Major();
        major.setId(1L);
        major.setName("待删除专业");
        major.setStatus("ACTIVE");

        when(studentRepository.countByMajorId(1L)).thenReturn(0L);
        when(majorRepository.findById(1L)).thenReturn(Optional.of(major));

        Map<String, Object> result = service.delete(1L);
        assertTrue((Boolean) result.get("success"));
        assertEquals("专业已停用", result.get("message"));
        assertEquals("INACTIVE", major.getStatus(), "应被软删除（设 INACTIVE）");
        verify(majorRepository).save(major);
    }

    @Test
    @DisplayName("delete: 存在关联学生时应拒绝删除")
    void delete_WithStudents_ShouldReturnFalse() {
        when(studentRepository.countByMajorId(1L)).thenReturn(10L);

        Map<String, Object> result = service.delete(1L);
        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("message").toString().contains("10 名学生"));
        verify(majorRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete: 专业不存在时应返回失败")
    void delete_NonExistent_ShouldReturnFalse() {
        when(studentRepository.countByMajorId(999L)).thenReturn(0L);
        when(majorRepository.findById(999L)).thenReturn(Optional.empty());

        Map<String, Object> result = service.delete(999L);
        assertFalse((Boolean) result.get("success"));
        assertEquals("专业不存在", result.get("message"));
    }

    // ================================================================
    // list — 多条件分页查询
    // ================================================================

    @Test
    @DisplayName("list: 无筛选条件时应返回全部")
    void list_NoFilters_ShouldReturnAll() {
        Major m1 = new Major(); m1.setId(1L); m1.setName("专业A");
        Major m2 = new Major(); m2.setId(2L); m2.setName("专业B");
        Page<Major> page = new PageImpl<>(List.of(m1, m2), PageRequest.of(0, 10), 2);

        when(majorRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Map<String, Object> result = service.list(null, null, "all", 0, 10, "id", "asc");
        assertEquals(2L, result.get("totalElements"));
        assertEquals(0, result.get("currentPage"));
    }

    @Test
    @DisplayName("list: 按学院筛选")
    void list_ByCollegeId_ShouldFilter() {
        Major m1 = new Major(); m1.setId(1L); m1.setName("专业A");
        Page<Major> page = new PageImpl<>(List.of(m1), PageRequest.of(0, 10), 1);

        when(majorRepository.findByCollegeId(eq(1L), any(PageRequest.class))).thenReturn(page);

        Map<String, Object> result = service.list(null, 1L, "all", 0, 10, "id", "asc");
        assertEquals(1L, result.get("totalElements"));
    }

    @Test
    @DisplayName("list: 按名称模糊查询")
    void list_ByName_ShouldFilter() {
        Major m1 = new Major(); m1.setId(1L); m1.setName("计算机科学");
        Page<Major> page = new PageImpl<>(List.of(m1), PageRequest.of(0, 10), 1);

        when(majorRepository.findByNameContaining(eq("计算机"), any(PageRequest.class))).thenReturn(page);

        Map<String, Object> result = service.list("计算机", null, "all", 0, 10, "id", "asc");
        assertEquals(1L, result.get("totalElements"));
    }

    @Test
    @DisplayName("list: 按名称+学院组合筛选")
    void list_ByNameAndCollegeId_ShouldFilter() {
        Major m1 = new Major(); m1.setId(1L); m1.setName("计算机科学");
        Page<Major> page = new PageImpl<>(List.of(m1), PageRequest.of(0, 10), 1);

        when(majorRepository.findByNameContainingAndCollegeId(eq("计算机"), eq(1L), any(PageRequest.class)))
                .thenReturn(page);

        Map<String, Object> result = service.list("计算机", 1L, "all", 0, 10, "id", "asc");
        assertEquals(1L, result.get("totalElements"));
    }

    @Test
    @DisplayName("list: 按学院+状态筛选（内存分页路径）")
    void list_ByCollegeIdAndStatus_ShouldFilterInMemory() {
        Major active = new Major(); active.setId(1L); active.setName("专业A"); active.setStatus("ACTIVE");
        Major inactive = new Major(); inactive.setId(2L); inactive.setName("专业B"); inactive.setStatus("INACTIVE");

        when(majorRepository.findByCollegeId(1L)).thenReturn(List.of(active, inactive));

        Map<String, Object> result = service.list(null, 1L, "ACTIVE", 0, 10, "id", "asc");
        assertEquals(1L, result.get("totalElements"));
        @SuppressWarnings("unchecked")
        List<Major> content = (List<Major>) result.get("content");
        assertEquals(1, content.size());
        assertEquals("专业A", content.get(0).getName());
    }

    @Test
    @DisplayName("list: 内存分页翻页应正确计算偏移")
    void list_ByCollegeIdAndStatus_Page2_ShouldCalculateOffset() {
        Major m1 = new Major(); m1.setId(1L); m1.setName("专业A"); m1.setStatus("ACTIVE");
        Major m2 = new Major(); m2.setId(2L); m2.setName("专业B"); m1.setStatus("ACTIVE");
        Major m3 = new Major(); m3.setId(3L); m3.setName("专业C"); m3.setStatus("ACTIVE");

        when(majorRepository.findByCollegeId(1L)).thenReturn(List.of(m1, m2, m3));

        // 每页2条，第2页（page=1）
        Map<String, Object> result = service.list(null, 1L, "ACTIVE", 1, 2, "id", "asc");
        assertEquals(3L, result.get("totalElements"));
        assertEquals(1, result.get("currentPage"));
        @SuppressWarnings("unchecked")
        List<Major> content = (List<Major>) result.get("content");
        assertEquals(1, content.size(), "第2页应只有1条（第3条）");
        assertEquals("专业C", content.get(0).getName());
    }

    @Test
    @DisplayName("list: 内存分页超出范围应返回空列表")
    void list_ByCollegeIdAndStatus_PageBeyondRange_ShouldReturnEmpty() {
        Major m1 = new Major(); m1.setId(1L); m1.setName("专业A"); m1.setStatus("ACTIVE");

        when(majorRepository.findByCollegeId(1L)).thenReturn(List.of(m1));

        // 请求第3页（超出范围）
        Map<String, Object> result = service.list(null, 1L, "ACTIVE", 2, 5, "id", "asc");
        assertEquals(1L, result.get("totalElements"));
        @SuppressWarnings("unchecked")
        List<Major> content = (List<Major>) result.get("content");
        assertTrue(content.isEmpty(), "超出范围的页应返回空列表");
    }

    // ================================================================
    // listByCollegeId — 仅 ACTIVE 专业
    // ================================================================

    @Test
    @DisplayName("listByCollegeId: 应仅返回 ACTIVE 状态的专业")
    void listByCollegeId_ShouldReturnOnlyActive() {
        Major active1 = new Major(); active1.setId(1L); active1.setName("专业A");
        Major active2 = new Major(); active2.setId(2L); active2.setName("专业B");

        when(majorRepository.findByCollegeIdAndStatus(1L, "ACTIVE"))
                .thenReturn(List.of(active1, active2));

        List<Major> result = service.listByCollegeId(1L);
        assertEquals(2, result.size());
    }

    // ================================================================
    // getById
    // ================================================================

    @Test
    @DisplayName("getById: 存在的 ID 应返回专业")
    void getById_Exists_ShouldReturnMajor() {
        Major major = new Major();
        major.setId(1L);
        major.setName("测试专业");

        when(majorRepository.findById(1L)).thenReturn(Optional.of(major));

        Major result = service.getById(1L);
        assertNotNull(result);
        assertEquals("测试专业", result.getName());
    }

    @Test
    @DisplayName("getById: 不存在的 ID 应返回 null")
    void getById_NotExists_ShouldReturnNull() {
        when(majorRepository.findById(999L)).thenReturn(Optional.empty());

        Major result = service.getById(999L);
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
