package com.labcourse;

import com.labcourse.entity.Student;
import com.labcourse.entity.Teacher;
import com.labcourse.entity.Course;
import com.labcourse.entity.Lab;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.LabRepository;
import com.labcourse.service.impl.CourseServiceImpl;
import com.labcourse.service.impl.LabServiceImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 学院(college)字段 — 实体层 + 服务层测试
 * 验证 @Size 约束、partial update 行为
 */
class CollegeFieldTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ===== 原有测试 =====

    @Test
    void studentShouldHaveCollegeField() {
        Student student = new Student();
        assertNull(student.getCollege());
        
        student.setCollege("计算机科学与技术学院");
        assertEquals("计算机科学与技术学院", student.getCollege());
    }

    @Test
    void teacherShouldHaveCollegeField() {
        Teacher teacher = new Teacher();
        assertNull(teacher.getCollege());
        
        teacher.setCollege("数学与统计学院");
        assertEquals("数学与统计学院", teacher.getCollege());
    }

    @Test
    void courseShouldHaveCollegeField() {
        Course course = new Course();
        assertNull(course.getCollege());
        
        course.setCollege("信息工程学院");
        assertEquals("信息工程学院", course.getCollege());
    }

    @Test
    void labShouldHaveCollegeField() {
        Lab lab = new Lab();
        assertNull(lab.getCollege());
        
        lab.setCollege("物理与电子工程学院");
        assertEquals("物理与电子工程学院", lab.getCollege());
    }

    @Test
    void studentCollegeDefaultIsNull() {
        Student student = new Student();
        assertNull(student.getCollege(), "新建 Student 的 college 默认应为 null");
    }

    @Test
    void teacherCollegeDefaultIsNull() {
        Teacher teacher = new Teacher();
        assertNull(teacher.getCollege(), "新建 Teacher 的 college 默认应为 null");
    }

    // ===== 新增: @Size 约束验证 (RED — 当前无约束, 应失败) =====

    @Test
    void studentCollegeMax100Rejects200Chars() {
        Student student = new Student();
        student.setCollege("A".repeat(200));
        Set<ConstraintViolation<Student>> violations = validator.validate(student);
        assertFalse(violations.isEmpty(),
            "应检测到 college 超长(200 > 100)的违规 — 当前缺少 @Size 约束");
    }

    @Test
    void teacherCollegeMax100Rejects200Chars() {
        Teacher teacher = new Teacher();
        teacher.setCollege("B".repeat(200));
        Set<ConstraintViolation<Teacher>> violations = validator.validate(teacher);
        assertFalse(violations.isEmpty(),
            "应检测到 college 超长(200 > 100)的违规 — 当前缺少 @Size 约束");
    }

    @Test
    void courseCollegeMax100Rejects200Chars() {
        Course course = new Course();
        course.setCollege("C".repeat(200));
        Set<ConstraintViolation<Course>> violations = validator.validate(course);
        assertFalse(violations.isEmpty(),
            "应检测到 college 超长(200 > 100)的违规 — 当前缺少 @Size 约束");
    }

    @Test
    void labCollegeMax100Rejects200Chars() {
        Lab lab = new Lab();
        lab.setCollege("D".repeat(200));
        Set<ConstraintViolation<Lab>> violations = validator.validate(lab);
        assertFalse(violations.isEmpty(),
            "应检测到 college 超长(200 > 100)的违规 — 当前缺少 @Size 约束");
    }

    // ===== 新增: Course updateById partial update (RED — 当前直接 save 覆盖) =====

    @Test
    void courseUpdateByIdOnlyCollegeShouldNotOverwriteOtherFields() {
        Course existing = new Course();
        existing.setId(1L);
        existing.setCourseName("原课程名");
        existing.setTeacherId(10L);
        existing.setLabId(20L);
        existing.setCourseTime("周一 1-2节");
        existing.setMaxCount(30);
        existing.setCollege("原始学院");

        CourseRepository mockRepo = mock(CourseRepository.class);
        when(mockRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(mockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CourseServiceImpl service = new CourseServiceImpl();
        injectField(service, "courseRepository", mockRepo);

        Course update = new Course();
        update.setId(1L);
        update.setCollege("新学院");
        service.updateById(update);

        assertEquals("原课程名", existing.getCourseName(),
            "partial update 不应覆盖未传入的 courseName");
        assertEquals(10L, existing.getTeacherId(),
            "partial update 不应覆盖未传入的 teacherId");
        assertEquals(20L, existing.getLabId(),
            "partial update 不应覆盖未传入的 labId");
        assertEquals("新学院", existing.getCollege(),
            "college 应被更新");
    }

    @Test
    void courseUpdateByIdWithEmptyCollegeShouldClearCollege() {
        Course existing = new Course();
        existing.setId(2L);
        existing.setCourseName("课程B");
        existing.setCollege("原学院");

        CourseRepository mockRepo = mock(CourseRepository.class);
        when(mockRepo.findById(2L)).thenReturn(Optional.of(existing));
        when(mockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CourseServiceImpl service = new CourseServiceImpl();
        injectField(service, "courseRepository", mockRepo);

        Course update = new Course();
        update.setId(2L);
        update.setCollege("");
        service.updateById(update);

        assertEquals("", existing.getCollege(),
            "college 应被清空为空字符串");
        assertEquals("课程B", existing.getCourseName(),
            "其他字段不应被覆盖");
    }

    // ===== 新增: Lab updateById partial update (RED — 当前直接 save 覆盖) =====

    @Test
    void labUpdateByIdOnlyCollegeShouldNotOverwriteOtherFields() {
        Lab existing = new Lab();
        existing.setId(1L);
        existing.setLabName("原实验室名");
        existing.setLocation("A栋101");
        existing.setCapacity(50);
        existing.setCollege("原始学院");

        LabRepository mockRepo = mock(LabRepository.class);
        when(mockRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(mockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LabServiceImpl service = new LabServiceImpl();
        injectField(service, "labRepository", mockRepo);

        Lab update = new Lab();
        update.setId(1L);
        update.setCollege("新学院");
        service.updateById(update);

        assertEquals("原实验室名", existing.getLabName(),
            "partial update 不应覆盖未传入的 labName");
        assertEquals("A栋101", existing.getLocation(),
            "partial update 不应覆盖未传入的 location");
        assertEquals(50, existing.getCapacity(),
            "partial update 不应覆盖未传入的 capacity");
        assertEquals("新学院", existing.getCollege(),
            "college 应被更新");
    }

    @Test
    void labUpdateByIdWithEmptyCollegeShouldClearCollege() {
        Lab existing = new Lab();
        existing.setId(2L);
        existing.setLabName("实验室B");
        existing.setCollege("原学院");

        LabRepository mockRepo = mock(LabRepository.class);
        when(mockRepo.findById(2L)).thenReturn(Optional.of(existing));
        when(mockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LabServiceImpl service = new LabServiceImpl();
        injectField(service, "labRepository", mockRepo);

        Lab update = new Lab();
        update.setId(2L);
        update.setCollege("");
        service.updateById(update);

        assertEquals("", existing.getCollege(),
            "college 应被清空为空字符串");
        assertEquals("实验室B", existing.getLabName(),
            "其他字段不应被覆盖");
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