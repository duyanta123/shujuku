package com.labcourse.service;

import com.labcourse.entity.Course;
import com.labcourse.entity.Major;
import com.labcourse.entity.MajorRequiredCourse;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.MajorRepository;
import com.labcourse.repository.MajorRequiredCourseRepository;
import com.labcourse.service.impl.MajorRequiredCourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MajorRequiredCourseServiceImpl 单元测试 — 覆盖必修课绑定/解绑业务逻辑（原本零覆盖）
 *
 * 风险行为覆盖：
 * - bind: 课程不存在、非必修课类型、专业不存在、跨学院绑定拦截、重复绑定拦截、成功绑定
 * - unbind: 成功解绑（委托 Repository）
 * - listByMajor: JDBC 查询路径（受限于 JdbcTemplate mock，做基础验证）
 */
@SuppressWarnings("null")
class MajorRequiredCourseServiceImplTest {

    private MajorRequiredCourseServiceImpl service;
    private MajorRequiredCourseRepository majorRequiredCourseRepository;
    private CourseRepository courseRepository;
    private MajorRepository majorRepository;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        service = new MajorRequiredCourseServiceImpl();
        majorRequiredCourseRepository = mock(MajorRequiredCourseRepository.class);
        courseRepository = mock(CourseRepository.class);
        majorRepository = mock(MajorRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);

        injectField(service, "majorRequiredCourseRepository", majorRequiredCourseRepository);
        injectField(service, "courseRepository", courseRepository);
        injectField(service, "majorRepository", majorRepository);
        injectField(service, "jdbcTemplate", jdbcTemplate);
    }

    // ================================================================
    // bind — 课程不存在
    // ================================================================

    @Test
    @DisplayName("bind: 课程不存在应返回失败")
    void bind_CourseNotFound_ShouldReturnFalse() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        Map<String, Object> result = service.bind(1L, 99L);
        assertFalse((Boolean) result.get("success"));
        assertEquals("课程不存在", result.get("message"));
        verify(majorRequiredCourseRepository, never()).save(any());
    }

    // ================================================================
    // bind — 非必修课类型拒绝
    // ================================================================

    @Test
    @DisplayName("bind: 课程类型为选修课应拒绝绑定")
    void bind_ElectiveCourse_ShouldReturnFalse() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("ELECTIVE");
        course.setCourseName("Python编程");

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        Map<String, Object> result = service.bind(1L, 10L);
        assertFalse((Boolean) result.get("success"));
        assertEquals("仅可绑定必修课类型的课程", result.get("message"));
        verify(majorRequiredCourseRepository, never()).save(any());
    }

    @Test
    @DisplayName("bind: courseType 为 null 且不是 REQUIRED 应拒绝")
    void bind_CourseTypeNull_ShouldReturnFalse() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType(null);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        Map<String, Object> result = service.bind(1L, 10L);
        assertFalse((Boolean) result.get("success"));
        assertEquals("仅可绑定必修课类型的课程", result.get("message"));
    }

    // ================================================================
    // bind — 专业不存在
    // ================================================================

    @Test
    @DisplayName("bind: 专业不存在应返回失败")
    void bind_MajorNotFound_ShouldReturnFalse() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("REQUIRED");
        course.setCollegeId(1L);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(majorRepository.findById(99L)).thenReturn(Optional.empty());

        Map<String, Object> result = service.bind(99L, 10L);
        assertFalse((Boolean) result.get("success"));
        assertEquals("专业不存在", result.get("message"));
        verify(majorRequiredCourseRepository, never()).save(any());
    }

    // ================================================================
    // bind — 跨学院绑定拦截（关键安全逻辑）
    // ================================================================

    @Test
    @DisplayName("bind: 课程与专业属于不同学院应拒绝跨学院绑定")
    void bind_CrossCollege_ShouldReturnFalse() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("REQUIRED");
        course.setCollegeId(1L);  // 学院1
        course.setCourseName("高等数学");

        Major major = new Major();
        major.setId(5L);
        major.setCollegeId(2L);  // 学院2（不同学院）
        major.setName("计算机科学");

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(majorRepository.findById(5L)).thenReturn(Optional.of(major));

        Map<String, Object> result = service.bind(5L, 10L);
        assertFalse((Boolean) result.get("success"));
        assertEquals("不可跨学院绑定必修课，课程和专业必须属于同一学院", result.get("message"));
        verify(majorRequiredCourseRepository, never()).save(any());
    }

    @Test
    @DisplayName("bind: 课程 collegeId 为 null 时应拒绝（跨学院校验不通过）")
    void bind_CourseCollegeIdNull_ShouldReturnFalse() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("REQUIRED");
        course.setCollegeId(null);

        Major major = new Major();
        major.setId(5L);
        major.setCollegeId(1L);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(majorRepository.findById(5L)).thenReturn(Optional.of(major));

        Map<String, Object> result = service.bind(5L, 10L);
        assertFalse((Boolean) result.get("success"));
        assertEquals("不可跨学院绑定必修课，课程和专业必须属于同一学院", result.get("message"));
    }

    // ================================================================
    // bind — 重复绑定拦截
    // ================================================================

    @Test
    @DisplayName("bind: 同一专业已绑定相同课程时应拒绝重复绑定")
    void bind_DuplicateBinding_ShouldReturnFalse() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("REQUIRED");
        course.setCollegeId(1L);
        course.setCourseName("高等数学");

        Major major = new Major();
        major.setId(5L);
        major.setCollegeId(1L);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(majorRepository.findById(5L)).thenReturn(Optional.of(major));
        when(majorRequiredCourseRepository.findByMajorIdAndCourseId(5L, 10L))
                .thenReturn(Optional.of(new MajorRequiredCourse()));

        Map<String, Object> result = service.bind(5L, 10L);
        assertFalse((Boolean) result.get("success"));
        assertEquals("该专业已绑定此必修课", result.get("message"));
        verify(majorRequiredCourseRepository, never()).save(any());
    }

    // ================================================================
    // bind — 成功绑定
    // ================================================================

    @Test
    @DisplayName("bind: 同学院、必修课类型、无重复时应成功绑定")
    void bind_ValidInput_ShouldSucceed() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("REQUIRED");
        course.setCollegeId(1L);
        course.setCourseName("高等数学");

        Major major = new Major();
        major.setId(5L);
        major.setCollegeId(1L);
        major.setName("计算机科学");

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(majorRepository.findById(5L)).thenReturn(Optional.of(major));
        when(majorRequiredCourseRepository.findByMajorIdAndCourseId(5L, 10L))
                .thenReturn(Optional.empty());

        Map<String, Object> result = service.bind(5L, 10L);
        assertTrue((Boolean) result.get("success"));
        assertEquals("绑定成功", result.get("message"));

        // 验证保存的 MajorRequiredCourse 字段正确
        verify(majorRequiredCourseRepository).save(argThat(mrc ->
                mrc.getMajorId().equals(5L) && mrc.getCourseId().equals(10L)));
    }

    // ================================================================
    // unbind
    // ================================================================

    @Test
    @DisplayName("unbind: 应成功调用 Repository 删除")
    void unbind_ShouldCallRepository() {
        boolean result = service.unbind(5L, 10L);
        assertTrue(result);
        verify(majorRequiredCourseRepository).deleteByMajorIdAndCourseId(5L, 10L);
    }

    // ================================================================
    // listByMajor — JDBC 查询（基础验证）
    // ================================================================

    @Test
    @DisplayName("listByMajor: 应调用 JdbcTemplate 返回结果")
    void listByMajor_ShouldReturnJdbcResults() {
        Map<String, Object> row1 = Map.of(
                "id", 1, "major_id", 5L, "course_id", 10L,
                "course_name", "高等数学", "college", "信息工程学院",
                "course_time", "周一 1-2节", "max_count", 30,
                "teacher_name", "张教授"
        );
        when(jdbcTemplate.queryForList(anyString(), eq(5L))).thenReturn(List.of(row1));

        List<Map<String, Object>> result = service.listByMajor(5L);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("高等数学", result.get(0).get("course_name"));
    }

    @Test
    @DisplayName("listByMajor: 无绑定记录时应返回空列表")
    void listByMajor_NoBindings_ShouldReturnEmpty() {
        when(jdbcTemplate.queryForList(anyString(), eq(99L))).thenReturn(List.of());

        List<Map<String, Object>> result = service.listByMajor(99L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
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