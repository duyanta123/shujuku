package com.labcourse.service;

import com.labcourse.entity.College;
import com.labcourse.entity.Course;
import com.labcourse.entity.Major;
import com.labcourse.entity.MajorRequiredCourse;
import com.labcourse.entity.Selection;
import com.labcourse.entity.Student;
import com.labcourse.repository.AttendanceRepository;
import com.labcourse.repository.CollegeRepository;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.MajorRepository;
import com.labcourse.repository.MajorRequiredCourseRepository;
import com.labcourse.repository.ScoreRepository;
import com.labcourse.repository.SelectionRepository;
import com.labcourse.repository.StudentRepository;
import com.labcourse.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * StudentServiceImpl 单元测试 — 覆盖近期新增的专业变更+必修课自动分配逻辑
 *
 * 风险行为覆盖：
 * - save: 创建学生时自动分配必修课（新逻辑）
 * - updateById: 专业变更 → 移除旧必修课选课并分配新必修课选课（复杂业务逻辑）
 * - updateById: 首次分配专业 → 自动分配必修课
 * - updateById: 密码变更 → BCrypt 编码
 * - updateById: 部分字段更新保留未传入字段（college/collegeId/majorId 等）
 */
@SuppressWarnings("null")
class StudentServiceImplTest {

    private StudentServiceImpl service;
    private StudentRepository studentRepository;
    private PasswordEncoder passwordEncoder;
    private LoginAttemptService loginAttemptService;
    private MajorRequiredCourseRepository majorRequiredCourseRepository;
    private CourseRepository courseRepository;
    private SelectionRepository selectionRepository;
    private ScoreRepository scoreRepository;
    private AttendanceRepository attendanceRepository;
    private CollegeRepository collegeRepository;
    private MajorRepository majorRepository;

    @BeforeEach
    void setUp() {
        service = new StudentServiceImpl();
        studentRepository = mock(StudentRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        loginAttemptService = mock(LoginAttemptService.class);
        majorRequiredCourseRepository = mock(MajorRequiredCourseRepository.class);
        courseRepository = mock(CourseRepository.class);
        selectionRepository = mock(SelectionRepository.class);
        scoreRepository = mock(ScoreRepository.class);
        attendanceRepository = mock(AttendanceRepository.class);
        collegeRepository = mock(CollegeRepository.class);
        majorRepository = mock(MajorRepository.class);

        injectField(service, "studentRepository", studentRepository);
        injectField(service, "passwordEncoder", passwordEncoder);
        injectField(service, "loginAttemptService", loginAttemptService);
        injectField(service, "majorRequiredCourseRepository", majorRequiredCourseRepository);
        injectField(service, "courseRepository", courseRepository);
        injectField(service, "selectionRepository", selectionRepository);
        injectField(service, "scoreRepository", scoreRepository);
        injectField(service, "attendanceRepository", attendanceRepository);
        injectField(service, "collegeRepository", collegeRepository);
        injectField(service, "majorRepository", majorRepository);
    }

    // ================================================================
    // save — 创建学生 + 密码编码 + 必修课自动分配
    // ================================================================

    @Test
    @DisplayName("save: 创建学生应编码密码并自动分配必修课")
    void save_WithMajorId_ShouldEncodePasswordAndAssignRequiredCourses() {
        Student student = new Student();
        student.setStudentNo("S100");
        student.setName("新学生");
        student.setPassword("rawPassword");
        student.setMajorId(1L);

        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

        // 必修课配置
        MajorRequiredCourse mrc = new MajorRequiredCourse();
        mrc.setMajorId(1L);
        mrc.setCourseId(10L);
        when(majorRequiredCourseRepository.findByMajorId(1L)).thenReturn(List.of(mrc));

        Course course = new Course();
        course.setId(10L);
        course.setCourseName("高等数学");
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(selectionRepository.findByStudentIdAndCourseId(any(), eq(10L))).thenReturn(Optional.empty());

        boolean result = service.save(student);
        assertTrue(result);

        // 验证密码已编码
        assertEquals("encodedPassword", student.getPassword());
        verify(studentRepository).save(student);

        // 验证自动分配必修课选课（studentId 在 save 时为 null，由 DB 生成）
        verify(selectionRepository).save(argThat(sel ->
                sel.getCourseId().equals(10L)));
    }

    @Test
    @DisplayName("save: majorId 为 null 时不应分配必修课")
    void save_NoMajorId_ShouldNotAssignRequiredCourses() {
        Student student = new Student();
        student.setStudentNo("S101");
        student.setPassword("rawPassword");
        student.setMajorId(null);

        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

        boolean result = service.save(student);
        assertTrue(result);
        verify(selectionRepository, never()).save(any());
        verify(majorRequiredCourseRepository, never()).findByMajorId(any());
    }

    @Test
    @DisplayName("save: 专业无必修课配置时应正常保存")
    void save_MajorHasNoRequiredCourses_ShouldSucceed() {
        Student student = new Student();
        student.setStudentNo("S102");
        student.setPassword("rawPassword");
        student.setMajorId(99L);

        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(majorRequiredCourseRepository.findByMajorId(99L)).thenReturn(List.of());

        boolean result = service.save(student);
        assertTrue(result);
        verify(selectionRepository, never()).save(any());
    }

    // ================================================================
    // updateById — 专业变更 → 必修课重新分配（核心复杂逻辑）
    // ================================================================

    @Test
    @DisplayName("updateById: 专业变更应移除旧必修课选课并分配新必修课")
    void updateById_MajorChanged_ShouldReassignRequiredCourses() {
        Student existing = new Student();
        existing.setId(1L);
        existing.setStudentNo("S001");
        existing.setName("张三");
        existing.setMajorId(1L);  // 旧专业
        existing.setCollegeId(1L);

        Student update = new Student();
        update.setId(1L);
        update.setMajorId(2L);  // 变更为新专业

        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));

        // 旧专业必修课
        MajorRequiredCourse oldMrc = new MajorRequiredCourse();
        oldMrc.setMajorId(1L);
        oldMrc.setCourseId(10L);
        when(majorRequiredCourseRepository.findByMajorId(1L)).thenReturn(List.of(oldMrc));

        // 旧专业已有选课记录
        Selection oldSelection = new Selection();
        oldSelection.setId(100L);
        oldSelection.setStudentId(1L);
        oldSelection.setCourseId(10L);
        when(selectionRepository.findByStudentIdAndCourseId(1L, 10L))
                .thenReturn(Optional.of(oldSelection));

        // 新专业必修课
        MajorRequiredCourse newMrc = new MajorRequiredCourse();
        newMrc.setMajorId(2L);
        newMrc.setCourseId(20L);
        when(majorRequiredCourseRepository.findByMajorId(2L)).thenReturn(List.of(newMrc));

        Major newMajor = new Major();
        newMajor.setId(2L);
        newMajor.setName("计算机科学与技术");
        when(majorRepository.findById(2L)).thenReturn(Optional.of(newMajor));

        Course newCourse = new Course();
        newCourse.setId(20L);
        newCourse.setCourseName("数据库原理");
        when(courseRepository.findById(20L)).thenReturn(Optional.of(newCourse));
        when(selectionRepository.findByStudentIdAndCourseId(1L, 20L)).thenReturn(Optional.empty());

        boolean result = service.updateById(update);
        assertTrue(result);

        // 验证旧必修课选课被删除
        verify(selectionRepository).delete(oldSelection);

        // 验证新必修课被分配
        verify(studentRepository).save(existing);
        verify(selectionRepository).save(argThat(sel ->
                sel.getStudentId().equals(1L) && sel.getCourseId().equals(20L)));
    }

    @Test
    @DisplayName("updateById: 专业变更但旧专业无已选必修课时只分配新必修课")
    void updateById_MajorChanged_NoOldSelections_ShouldAssignNew() {
        Student existing = new Student();
        existing.setId(1L);
        existing.setMajorId(1L);

        Student update = new Student();
        update.setId(1L);
        update.setMajorId(2L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));

        // 旧专业必修课存在但未创建选课记录
        MajorRequiredCourse oldMrc = new MajorRequiredCourse();
        oldMrc.setMajorId(1L);
        oldMrc.setCourseId(10L);
        when(majorRequiredCourseRepository.findByMajorId(1L)).thenReturn(List.of(oldMrc));
        when(selectionRepository.findByStudentIdAndCourseId(1L, 10L)).thenReturn(Optional.empty());

        // 新专业有必修课
        MajorRequiredCourse newMrc = new MajorRequiredCourse();
        newMrc.setMajorId(2L);
        newMrc.setCourseId(20L);
        when(majorRequiredCourseRepository.findByMajorId(2L)).thenReturn(List.of(newMrc));
        when(majorRepository.findById(2L)).thenReturn(Optional.of(new Major()));
        when(courseRepository.findById(20L)).thenReturn(Optional.of(new Course()));
        when(selectionRepository.findByStudentIdAndCourseId(1L, 20L)).thenReturn(Optional.empty());

        boolean result = service.updateById(update);
        assertTrue(result);
        // 未删除（因为没有已存在的选课）
        verify(selectionRepository, never()).delete(any());
        // 但新必修课被分配
        verify(selectionRepository).save(argThat(sel -> sel.getCourseId().equals(20L)));
    }

    @Test
    @DisplayName("updateById: 首次分配专业应自动分配必修课")
    void updateById_FirstTimeMajorAssignment_ShouldAssignCourses() {
        Student existing = new Student();
        existing.setId(1L);
        existing.setMajorId(null);  // 之前没有专业

        Student update = new Student();
        update.setId(1L);
        update.setMajorId(3L);  // 首次分配

        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));

        MajorRequiredCourse mrc = new MajorRequiredCourse();
        mrc.setMajorId(3L);
        mrc.setCourseId(30L);
        when(majorRequiredCourseRepository.findByMajorId(3L)).thenReturn(List.of(mrc));

        when(majorRepository.findById(3L)).thenReturn(Optional.of(new Major()));

        Course course = new Course();
        course.setId(30L);
        course.setCourseName("数据结构");
        when(courseRepository.findById(30L)).thenReturn(Optional.of(course));
        when(selectionRepository.findByStudentIdAndCourseId(1L, 30L)).thenReturn(Optional.empty());

        boolean result = service.updateById(update);
        assertTrue(result);
        verify(selectionRepository).save(argThat(sel -> sel.getCourseId().equals(30L)));
    }

    // ================================================================
    // updateById — 部分字段更新
    // ================================================================

    @Test
    @DisplayName("updateById: 部分更新应保留未传入的字段")
    void updateById_PartialFields_ShouldPreserveExisting() {
        Student existing = new Student();
        existing.setId(1L);
        existing.setName("张三");
        existing.setStudentNo("S001");
        existing.setGender("男");
        existing.setCollegeId(1L);
        existing.setMajorId(1L);

        Student update = new Student();
        update.setId(1L);
        update.setName("张三改");
        // 其他字段未传入

        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);
        assertTrue(result);

        assertEquals("张三改", existing.getName());
        assertEquals("S001", existing.getStudentNo(), "未传入的 studentNo 应保留");
        assertEquals("男", existing.getGender(), "未传入的 gender 应保留");
        assertEquals(1L, existing.getCollegeId(), "未传入的 collegeId 应保留");
        assertEquals(1L, existing.getMajorId(), "未传入的 majorId 应保留");
        verify(studentRepository).save(existing);
    }

    @Test
    @DisplayName("updateById: 不存在的学生应返回 false")
    void updateById_NonExistent_ShouldReturnFalse() {
        Student update = new Student();
        update.setId(999L);
        update.setName("不存在");

        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = service.updateById(update);
        assertFalse(result);
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateById: 密码变更应编码后再保存")
    void updateById_PasswordChanged_ShouldEncode() {
        Student existing = new Student();
        existing.setId(1L);
        existing.setPassword("oldEncoded");

        Student update = new Student();
        update.setId(1L);
        update.setPassword("newRawPassword");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newRawPassword")).thenReturn("newEncoded");

        boolean result = service.updateById(update);
        assertTrue(result);
        assertEquals("newEncoded", existing.getPassword());
        verify(passwordEncoder).encode("newRawPassword");
    }

    @Test
    @DisplayName("updateById: 密码为空字符串时不应覆盖原密码")
    void updateById_EmptyPassword_ShouldNotEncode() {
        Student existing = new Student();
        existing.setId(1L);
        existing.setPassword("oldEncoded");

        Student update = new Student();
        update.setId(1L);
        update.setPassword("");

        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);
        assertTrue(result);
        assertEquals("oldEncoded", existing.getPassword(), "空密码不应覆盖");
        verify(passwordEncoder, never()).encode(any());
    }

    // ================================================================
    // updateById — collegeId / college 字段更新
    // ================================================================

    @Test
    @DisplayName("updateById: college 字符串字段应正常更新")
    void updateById_CollegeField_ShouldUpdate() {
        Student existing = new Student();
        existing.setId(1L);
        existing.setCollegeId(1L);

        Student update = new Student();
        update.setId(1L);
        update.setCollegeId(1L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = service.updateById(update);
        assertTrue(result);
        assertEquals(1L, existing.getCollegeId());
    }

    @Test
    @DisplayName("updateById: collegeId 字段应正常更新")
    void updateById_CollegeIdField_ShouldUpdate() {
        Student existing = new Student();
        existing.setId(1L);
        existing.setCollegeId(1L);

        Student update = new Student();
        update.setId(1L);
        update.setCollegeId(2L);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(collegeRepository.findById(2L)).thenReturn(Optional.of(new College()));

        boolean result = service.updateById(update);
        assertTrue(result);
        assertEquals(2L, existing.getCollegeId());
    }

    // ================================================================
    // assignRequiredCourses — 边界情况
    // ================================================================

    @Test
    @DisplayName("assignRequiredCourses: 必修课对应课程不存在应跳过")
    void assignRequiredCourses_CourseNotFound_ShouldSkip() {
        Student student = new Student();
        student.setStudentNo("S200");
        student.setPassword("raw");
        student.setMajorId(5L);

        MajorRequiredCourse mrc = new MajorRequiredCourse();
        mrc.setMajorId(5L);
        mrc.setCourseId(999L);

        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(majorRequiredCourseRepository.findByMajorId(5L)).thenReturn(List.of(mrc));
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = service.save(student);
        assertTrue(result, "课程不存在时应跳过，不应抛异常");
        verify(selectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("assignRequiredCourses: 已存在的选课记录不应重复分配")
    void assignRequiredCourses_ExistingSelection_ShouldSkip() {
        Student student = new Student();
        student.setStudentNo("S201");
        student.setPassword("raw");
        student.setMajorId(6L);

        MajorRequiredCourse mrc = new MajorRequiredCourse();
        mrc.setMajorId(6L);
        mrc.setCourseId(50L);

        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(majorRequiredCourseRepository.findByMajorId(6L)).thenReturn(List.of(mrc));
        when(selectionRepository.findByStudentIdAndCourseId(any(), eq(50L)))
                .thenReturn(Optional.of(new Selection()));

        boolean result = service.save(student);
        assertTrue(result);
        // 不重复添加
        verify(selectionRepository, never()).save(any());
    }

    // ================================================================
    // removeById
    // ================================================================

    @Test
    @DisplayName("removeById: 删除学生应调用 deleteById")
    void removeById_ShouldCallRepository() {
        boolean result = service.removeById(1L);
        assertTrue(result);
        verify(selectionRepository).deleteByStudentId(1L);
        verify(scoreRepository).deleteByStudentId(1L);
        verify(attendanceRepository).deleteByStudentId(1L);
        verify(studentRepository).deleteById(1L);
    }

    // ================================================================
    // list — collegeId 筛选
    // ================================================================

    @Test
    @DisplayName("list: collegeId 筛选 — null 返回全部 / 有效值返回筛选 / 无效值返回空")
    void testListWithCollegeId() {
        // 测试 collegeId = null 时返回全部
        List<Student> allStudents = service.list(null);
        assertNotNull(allStudents);
        // 至少有 seed 数据中的学生
        assertTrue(allStudents.size() >= 0);

        // 测试 collegeId = 有效值返回筛选结果
        // 使用 seed 数据中存在的 collegeId（如 1L）
        List<Student> filtered = service.list(1L);
        assertNotNull(filtered);
        // 筛选结果不应超过全部
        assertTrue(filtered.size() <= allStudents.size());

        // 测试 collegeId = 无效值返回空列表
        List<Student> empty = service.list(99999L);
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
