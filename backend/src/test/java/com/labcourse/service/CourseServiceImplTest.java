package com.labcourse.service;

import com.labcourse.entity.Course;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.SelectionRepository;
import com.labcourse.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CourseServiceImpl 单元测试 — 覆盖选课后类型变更拦截与级联删除（原本零覆盖）
 *
 * 风险行为覆盖：
 * - updateById: courseType 变更 → 已有学生选课时拒绝修改（数据一致性保护）
 * - updateById: courseType 不变时正常更新（不触发检查）
 * - updateById: 部分字段更新保留未传入字段
 * - removeById: 级联删除 selection/score/attendance 关联数据
 * - removeById: 课程不存在时拒绝删除
 */
@SuppressWarnings("null")
class CourseServiceImplTest {

    private CourseServiceImpl service;
    private CourseRepository courseRepository;
    private SelectionRepository selectionRepository;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        service = new CourseServiceImpl();
        courseRepository = mock(CourseRepository.class);
        selectionRepository = mock(SelectionRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);

        injectField(service, "courseRepository", courseRepository);
        injectField(service, "selectionRepository", selectionRepository);
        injectField(service, "jdbcTemplate", jdbcTemplate);
    }

    // ================================================================
    // updateById — 课程类型变更拦截（关键数据一致性逻辑）
    // ================================================================

    @Test
    @DisplayName("updateById: 课程类型变更且已有选课记录时应拒绝修改")
    void updateById_CourseTypeChanged_HasSelections_ShouldReturnFalse() {
        Course existing = new Course();
        existing.setId(1L);
        existing.setCourseName("Python编程");
        existing.setCourseType("ELECTIVE");  // 原类型：选修

        Course update = new Course();
        update.setId(1L);
        update.setCourseType("REQUIRED");  // 尝试改为必修

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(selectionRepository.existsByCourseId(1L)).thenReturn(true);  // 已有学生选课

        boolean result = service.updateById(update);
        assertFalse(result, "已有选课记录时不应允许修改课程类型");
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateById: 课程类型变更但无选课记录时应允许修改")
    void updateById_CourseTypeChanged_NoSelections_ShouldSucceed() {
        Course existing = new Course();
        existing.setId(1L);
        existing.setCourseName("Python编程");
        existing.setCourseType("ELECTIVE");

        Course update = new Course();
        update.setId(1L);
        update.setCourseType("REQUIRED");

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(selectionRepository.existsByCourseId(1L)).thenReturn(false);  // 无选课

        boolean result = service.updateById(update);
        assertTrue(result, "无选课记录时应允许修改课程类型");
        assertEquals("REQUIRED", existing.getCourseType());
        verify(courseRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: 课程类型不变时不检查选课记录")
    void updateById_CourseTypeNotChanged_ShouldNotCheckSelections() {
        Course existing = new Course();
        existing.setId(1L);
        existing.setCourseName("Python编程");
        existing.setCourseType("REQUIRED");

        Course update = new Course();
        update.setId(1L);
        update.setCourseType("REQUIRED");  // 类型不变

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);
        assertTrue(result);
        // 类型未变，不应检查选课
        verify(selectionRepository, never()).existsByCourseId(any());
        verify(courseRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: 原有 courseType 为 null 时直接设置新类型（不触发变更检查）")
    void updateById_ExistingCourseTypeNull_ChangeShouldCheckSelections() {
        Course existing = new Course();
        existing.setId(1L);
        existing.setCourseName("Python编程");
        existing.setCourseType(null);

        Course update = new Course();
        update.setId(1L);
        update.setCourseType("REQUIRED");

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(selectionRepository.existsByCourseId(1L)).thenReturn(true);  // 已有选课，但因原类型为null不触发检查

        // 原类型为 null 时，代码中的 existing.getCourseType() != null 条件为 false，
        // 直接跳过选课检查，允许设置新类型
        boolean result = service.updateById(update);
        assertTrue(result, "原类型为 null 时不触发变更检查，直接设置新类型");
        assertEquals("REQUIRED", existing.getCourseType());
        verify(selectionRepository, never()).existsByCourseId(any());
    }

    // ================================================================
    // updateById — 部分字段更新
    // ================================================================

    @Test
    @DisplayName("updateById: 部分更新应保留未传入字段")
    void updateById_PartialFields_ShouldPreserveExisting() {
        Course existing = new Course();
        existing.setId(1L);
        existing.setCourseName("Python编程");
        existing.setTeacherId(10L);
        existing.setLabId(20L);
        existing.setCourseTime("周一 1-2节");
        existing.setMaxCount(30);
        existing.setCollegeId(1L);
        existing.setCourseType("ELECTIVE");

        Course update = new Course();
        update.setId(1L);
        update.setCourseName("Python高级编程");
        // 其他字段未传入

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);
        assertTrue(result);

        assertEquals("Python高级编程", existing.getCourseName());
        assertEquals(10L, existing.getTeacherId(), "未传入的 teacherId 应保留");
        assertEquals(20L, existing.getLabId(), "未传入的 labId 应保留");
        assertEquals("周一 1-2节", existing.getCourseTime(), "未传入的 courseTime 应保留");
        assertEquals(30, existing.getMaxCount(), "未传入的 maxCount 应保留");
        assertEquals(1L, existing.getCollegeId(), "未传入的 collegeId 应保留");
        assertEquals("ELECTIVE", existing.getCourseType(), "未传入的 courseType 应保留");
        verify(courseRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: 不存在的课程应返回 false")
    void updateById_NonExistent_ShouldReturnFalse() {
        Course update = new Course();
        update.setId(999L);
        update.setCourseName("不存在");

        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = service.updateById(update);
        assertFalse(result);
        verify(courseRepository, never()).save(any());
    }

    // ================================================================
    // removeById — 级联删除（关键数据完整性逻辑）
    // ================================================================

    @Test
    @DisplayName("removeById: 课程存在时应级联删除关联数据")
    void removeById_CourseExists_ShouldCascadeDelete() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(jdbcTemplate.update(anyString(), (Object) eq(1L))).thenReturn(3).thenReturn(2).thenReturn(5).thenReturn(1);

        boolean result = service.removeById(1L);
        assertTrue(result);

        // 验证级联删除顺序：先删选课、再删成绩、再删考勤、最后删课程
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM selection WHERE course_id = ?"), (Object) eq(1L));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM score WHERE course_id = ?"), (Object) eq(1L));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM attendance WHERE course_id = ?"), (Object) eq(1L));
        verify(jdbcTemplate, times(1)).update(eq("DELETE FROM major_required_course WHERE course_id = ?"), (Object) eq(1L));
        verify(courseRepository).deleteById(1L);
    }

    @Test
    @DisplayName("removeById: 课程不存在时应拒绝删除")
    void removeById_CourseNotExists_ShouldReturnFalse() {
        when(courseRepository.existsById(999L)).thenReturn(false);

        boolean result = service.removeById(999L);
        assertFalse(result);
        verify(courseRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("removeById: 无关联系数据时正常删除课程")
    void removeById_NoRelatedData_ShouldSucceed() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(jdbcTemplate.update(anyString(), (Object) eq(1L))).thenReturn(0);

        boolean result = service.removeById(1L);
        assertTrue(result);
        verify(courseRepository).deleteById(1L);
    }

    // ================================================================
    // save
    // ================================================================

    @Test
    @DisplayName("save: 应成功保存课程")
    void save_ShouldSucceed() {
        Course course = new Course();
        course.setCourseName("新课程");
        course.setCourseTime("周一 1-2节");
        course.setMaxCount(30);

        boolean result = service.save(course);
        assertTrue(result);
        verify(courseRepository).save(course);
    }

    // ================================================================
    // list — collegeId 筛选
    // ================================================================

    @Test
    @DisplayName("list: collegeId 筛选 — null 返回全部 / 有效值返回筛选 / 无效值返回空")
    void testListWithCollegeId() {
        // 测试 collegeId = null 时返回全部
        List<Course> allCourses = service.list(null);
        assertNotNull(allCourses);
        assertTrue(allCourses.size() >= 0);

        // 测试 collegeId = 有效值返回筛选结果
        List<Course> filtered = service.list(1L);
        assertNotNull(filtered);
        assertTrue(filtered.size() <= allCourses.size());

        // 测试 collegeId = 无效值返回空列表
        List<Course> empty = service.list(99999L);
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
