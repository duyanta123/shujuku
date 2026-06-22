package com.labcourse.service;

import com.labcourse.entity.Course;
import com.labcourse.entity.Selection;
import com.labcourse.entity.Student;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.SelectionRepository;
import com.labcourse.repository.StudentRepository;
import com.labcourse.service.impl.SelectionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SelectionServiceImpl 单元测试 — 覆盖近期新增的 IDOR 防护与学院校验逻辑
 *
 * 风险行为覆盖：
 * - addSelection: 必修课拒绝手动选课、跨学院选课拦截、课程满员拦截、重复选课拦截
 * - deleteSelection: IDOR 防护（仅选课所有者可退课）、不存在的记录
 */
@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SelectionServiceImplTest {

    @InjectMocks
    private SelectionServiceImpl service;

    @Mock
    private SelectionRepository selectionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    // ================================================================
    // addSelection — 课程不存在
    // ================================================================

    @Test
    @DisplayName("addSelection: 课程不存在应返回 false")
    void addSelection_CourseNotFound_ShouldReturnFalse() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = service.addSelection(1L, 99L);
        assertFalse(result);
        verify(selectionRepository, never()).save(any());
    }

    // ================================================================
    // addSelection — 必修课拒绝手动选课（新增逻辑）
    // ================================================================

    @Test
    @DisplayName("addSelection: 必修课类型应拒绝手动选课")
    void addSelection_RequiredCourse_ShouldReturnFalse() {
        Course requiredCourse = new Course();
        requiredCourse.setId(10L);
        requiredCourse.setCourseType("REQUIRED");
        requiredCourse.setCourseName("高等数学");

        when(courseRepository.findById(10L)).thenReturn(Optional.of(requiredCourse));

        boolean result = service.addSelection(1L, 10L);
        assertFalse(result, "必修课由系统自动分配，不应接受手动选课");
        verify(selectionRepository, never()).save(any());
    }

    // ================================================================
    // addSelection — 学院不匹配拦截（新增安全逻辑）
    // ================================================================

    @Test
    @DisplayName("addSelection: 学生与课程学院不匹配应拒绝选课")
    void addSelection_CollegeMismatch_ShouldReturnFalse() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("ELECTIVE");
        course.setCollegeId(1L);
        course.setMaxCount(30);

        Student student = new Student();
        student.setId(2L);
        student.setCollegeId(2L);  // 不同学院

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student));

        boolean result = service.addSelection(2L, 10L);
        assertFalse(result, "跨学院选课应被拒绝");
        verify(selectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("addSelection: 学院匹配时选课应成功（存储过程返回 result_code=0）")
    void addSelection_CollegeMatch_ShouldSucceed() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("ELECTIVE");
        course.setCollegeId(1L);
        course.setMaxCount(30);

        Student student = new Student();
        student.setId(2L);
        student.setCollegeId(1L);  // 同学院

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student));

        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_result_code", 0);
        procResult.put("p_result_msg", "选课成功");

        try (MockedConstruction<SimpleJdbcCall> mocked = mockConstruction(SimpleJdbcCall.class,
                (mock, ctx) -> {
                    when(mock.withProcedureName(anyString())).thenReturn(mock);
                    when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                    when(mock.execute(any(SqlParameterSource.class))).thenReturn(procResult);
                })) {
            boolean result = service.addSelection(2L, 10L);
            assertTrue(result, "同学院选课应成功");
            verify(selectionRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("addSelection: 课程无 collegeId 时跳过学院校验（存储过程返回 result_code=0）")
    void addSelection_CourseNoCollegeId_ShouldSkipCollegeCheck() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("ELECTIVE");
        course.setCollegeId(null);  // 未设置学院
        course.setMaxCount(30);

        Student student = new Student();
        student.setId(2L);
        student.setCollegeId(1L);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student));

        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_result_code", 0);
        procResult.put("p_result_msg", "选课成功");

        try (MockedConstruction<SimpleJdbcCall> mocked = mockConstruction(SimpleJdbcCall.class,
                (mock, ctx) -> {
                    when(mock.withProcedureName(anyString())).thenReturn(mock);
                    when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                    when(mock.execute(any(SqlParameterSource.class))).thenReturn(procResult);
                })) {
            boolean result = service.addSelection(2L, 10L);
            assertTrue(result, "课程无学院时应跳过学院校验");
        }
    }

    // ================================================================
    // addSelection — 重复选课拦截（存储过程返回 result_code=1）
    // ================================================================

    @Test
    @DisplayName("addSelection: 已选课程不应重复添加（存储过程返回 result_code=1）")
    void addSelection_DuplicateSelection_ShouldReturnFalse() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("ELECTIVE");

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_result_code", 1);
        procResult.put("p_result_msg", "已选过该课程");

        try (MockedConstruction<SimpleJdbcCall> mocked = mockConstruction(SimpleJdbcCall.class,
                (mock, ctx) -> {
                    when(mock.withProcedureName(anyString())).thenReturn(mock);
                    when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                    when(mock.execute(any(SqlParameterSource.class))).thenReturn(procResult);
                })) {
            boolean result = service.addSelection(1L, 10L);
            assertFalse(result, "重复选课应被拒绝");
            verify(selectionRepository, never()).save(any());
        }
    }

    // ================================================================
    // addSelection — 课程满员拦截（存储过程返回 result_code=2）
    // ================================================================

    @Test
    @DisplayName("addSelection: 课程满员应拒绝新选课（存储过程返回 result_code=2）")
    void addSelection_CourseFull_ShouldReturnFalse() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("ELECTIVE");
        course.setMaxCount(2);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_result_code", 2);
        procResult.put("p_result_msg", "课程容量已满");

        try (MockedConstruction<SimpleJdbcCall> mocked = mockConstruction(SimpleJdbcCall.class,
                (mock, ctx) -> {
                    when(mock.withProcedureName(anyString())).thenReturn(mock);
                    when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                    when(mock.execute(any(SqlParameterSource.class))).thenReturn(procResult);
                })) {
            boolean result = service.addSelection(1L, 10L);
            assertFalse(result, "满员课程不应接受新选课");
            verify(selectionRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("addSelection: 课程最后一个名额应选课成功（存储过程返回 result_code=0）")
    void addSelection_LastSlot_ShouldSucceed() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("ELECTIVE");
        course.setMaxCount(30);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_result_code", 0);
        procResult.put("p_result_msg", "选课成功");

        try (MockedConstruction<SimpleJdbcCall> mocked = mockConstruction(SimpleJdbcCall.class,
                (mock, ctx) -> {
                    when(mock.withProcedureName(anyString())).thenReturn(mock);
                    when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                    when(mock.execute(any(SqlParameterSource.class))).thenReturn(procResult);
                })) {
            boolean result = service.addSelection(1L, 10L);
            assertTrue(result, "最后一个名额应能成功选课");
            verify(selectionRepository, never()).save(any());
        }
    }

    // ================================================================
    // addSelection — 存储过程返回未知结果码
    // ================================================================

    @Test
    @DisplayName("addSelection: 存储过程返回未知结果码 99 应拒绝选课")
    void addSelection_UnexpectedResultCode_ShouldReturnFalse() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("ELECTIVE");

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_result_code", 99);
        procResult.put("p_result_msg", "未知错误");

        try (MockedConstruction<SimpleJdbcCall> mocked = mockConstruction(SimpleJdbcCall.class,
                (mock, ctx) -> {
                    when(mock.withProcedureName(anyString())).thenReturn(mock);
                    when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                    when(mock.execute(any(SqlParameterSource.class))).thenReturn(procResult);
                })) {
            boolean result = service.addSelection(1L, 10L);
            assertFalse(result, "未知结果码应被拒绝");
            verify(selectionRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("addSelection: 存储过程返回 null 结果码应拒绝选课")
    void addSelection_NullResultCode_ShouldReturnFalse() {
        Course course = new Course();
        course.setId(10L);
        course.setCourseType("ELECTIVE");

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_result_code", null);
        procResult.put("p_result_msg", null);

        try (MockedConstruction<SimpleJdbcCall> mocked = mockConstruction(SimpleJdbcCall.class,
                (mock, ctx) -> {
                    when(mock.withProcedureName(anyString())).thenReturn(mock);
                    when(mock.declareParameters(any(SqlParameter[].class))).thenReturn(mock);
                    when(mock.execute(any(SqlParameterSource.class))).thenReturn(procResult);
                })) {
            boolean result = service.addSelection(1L, 10L);
            assertFalse(result, "null 结果码应被拒绝");
            verify(selectionRepository, never()).save(any());
        }
    }

    // ================================================================
    // deleteSelection — IDOR 防护（关键安全测试）
    // ================================================================

    @Test
    @DisplayName("deleteSelection: 选课所有者可以退课")
    void deleteSelection_Owner_ShouldSucceed() {
        Selection selection = new Selection();
        selection.setId(100L);
        selection.setStudentId(3L);

        when(selectionRepository.findById(100L)).thenReturn(Optional.of(selection));

        boolean result = service.deleteSelection(100L, 3L);
        assertTrue(result, "选课所有者应能退课");
        verify(selectionRepository).deleteById(100L);
    }

    @Test
    @DisplayName("deleteSelection: 非选课所有者退课应拒绝（IDOR 防护）")
    void deleteSelection_NotOwner_ShouldReturnFalse() {
        Selection selection = new Selection();
        selection.setId(100L);
        selection.setStudentId(3L);   // 此选课属于学生3

        when(selectionRepository.findById(100L)).thenReturn(Optional.of(selection));

        // 学生5 试图删除学生3 的选课 — 应被拒绝
        boolean result = service.deleteSelection(100L, 5L);
        assertFalse(result, "IDOR: 非选课所有者不应能退课");
        verify(selectionRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteSelection: 不存在的选课记录应拒绝")
    void deleteSelection_NonExistent_ShouldReturnFalse() {
        when(selectionRepository.findById(9999L)).thenReturn(Optional.empty());

        boolean result = service.deleteSelection(9999L, 1L);
        assertFalse(result, "不存在的选课记录不应能退课");
        verify(selectionRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteSelection: 删除后 studentId 正确保存的选课记录")
    void deleteSelection_SavedCorrectly_VerifiesStudentId() {
        Selection selection = new Selection();
        selection.setId(200L);
        selection.setStudentId(7L);

        when(selectionRepository.findById(200L)).thenReturn(Optional.of(selection));

        service.deleteSelection(200L, 7L);
        verify(selectionRepository).deleteById(200L);
    }
}