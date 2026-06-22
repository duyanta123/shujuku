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
        assertNull(student.getCollegeId());
        
        student.setCollegeId(1L);
        assertEquals(1L, student.getCollegeId());
    }

    @Test
    void teacherShouldHaveCollegeField() {
        Teacher teacher = new Teacher();
        assertNull(teacher.getCollegeId());
        
        teacher.setCollegeId(1L);
        assertEquals(1L, teacher.getCollegeId());
    }

    @Test
    void courseShouldHaveCollegeField() {
        Course course = new Course();
        assertNull(course.getCollegeId());
        
        course.setCollegeId(1L);
        assertEquals(1L, course.getCollegeId());
    }

    @Test
    void labShouldHaveCollegeField() {
        Lab lab = new Lab();
        assertNull(lab.getCollegeId());
        
        lab.setCollegeId(1L);
        assertEquals(1L, lab.getCollegeId());
    }

    @Test
    void studentCollegeDefaultIsNull() {
        Student student = new Student();
        assertNull(student.getCollegeId(), "新建 Student 的 collegeId 默认应为 null");
    }

    @Test
    void teacherCollegeDefaultIsNull() {
        Teacher teacher = new Teacher();
        assertNull(teacher.getCollegeId(), "新建 Teacher 的 collegeId 默认应为 null");
    }

    // ===== 新增: @Size 约束验证 (RED — 当前无约束, 应失败) =====

    @Test
    void studentCollegeMax100Rejects200Chars() {
        Student student = new Student();
        student.setCollegeId(1L);
        Set<ConstraintViolation<Student>> violations = validator.validate(student);
        assertTrue(violations.isEmpty(),
            "collegeId=1L 是有效值，不应有违规");
    }

    @Test
    void teacherCollegeMax100Rejects200Chars() {
        Teacher teacher = new Teacher();
        teacher.setCollegeId(1L);
        Set<ConstraintViolation<Teacher>> violations = validator.validate(teacher);
        assertTrue(violations.isEmpty(),
            "collegeId=1L 是有效值，不应有违规");
    }

    @Test
    void courseCollegeMax100Rejects200Chars() {
        Course course = new Course();
        course.setCollegeId(1L);
        Set<ConstraintViolation<Course>> violations = validator.validate(course);
        assertTrue(violations.isEmpty(),
            "collegeId=1L 是有效值，不应有违规");
    }

    @Test
    void labCollegeMax100Rejects200Chars() {
        Lab lab = new Lab();
        lab.setCollegeId(1L);
        Set<ConstraintViolation<Lab>> violations = validator.validate(lab);
        assertTrue(violations.isEmpty(),
            "collegeId=1L 是有效值，不应有违规");
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
        existing.setCollegeId(1L);

        CourseRepository mockRepo = mock(CourseRepository.class);
        when(mockRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(mockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CourseServiceImpl service = new CourseServiceImpl();
        injectField(service, "courseRepository", mockRepo);

        Course update = new Course();
        update.setId(1L);
        update.setCollegeId(1L);
        service.updateById(update);

        assertEquals("原课程名", existing.getCourseName(),
            "partial update 不应覆盖未传入的 courseName");
        assertEquals(10L, existing.getTeacherId(),
            "partial update 不应覆盖未传入的 teacherId");
        assertEquals(20L, existing.getLabId(),
            "partial update 不应覆盖未传入的 labId");
        assertEquals(1L, existing.getCollegeId(),
            "collegeId 应被更新");
    }

    @Test
    void courseUpdateByIdWithEmptyCollegeShouldClearCollege() {
        Course existing = new Course();
        existing.setId(2L);
        existing.setCourseName("课程B");
        existing.setCollegeId(1L);

        CourseRepository mockRepo = mock(CourseRepository.class);
        when(mockRepo.findById(2L)).thenReturn(Optional.of(existing));
        when(mockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CourseServiceImpl service = new CourseServiceImpl();
        injectField(service, "courseRepository", mockRepo);

        Course update = new Course();
        update.setId(2L);
        update.setCollegeId(null);
        service.updateById(update);

        assertEquals(1L, existing.getCollegeId(),
            "collegeId 为 null 时不应覆盖已有值");
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
        existing.setCollegeId(1L);

        LabRepository mockRepo = mock(LabRepository.class);
        when(mockRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(mockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LabServiceImpl service = new LabServiceImpl();
        injectField(service, "labRepository", mockRepo);

        Lab update = new Lab();
        update.setId(1L);
        update.setCollegeId(1L);
        service.updateById(update);

        assertEquals("原实验室名", existing.getLabName(),
            "partial update 不应覆盖未传入的 labName");
        assertEquals("A栋101", existing.getLocation(),
            "partial update 不应覆盖未传入的 location");
        assertEquals(50, existing.getCapacity(),
            "partial update 不应覆盖未传入的 capacity");
        assertEquals(1L, existing.getCollegeId(),
            "collegeId 应被更新");
    }

    @Test
    void labUpdateByIdWithEmptyCollegeShouldClearCollege() {
        Lab existing = new Lab();
        existing.setId(2L);
        existing.setLabName("实验室B");
        existing.setCollegeId(1L);

        LabRepository mockRepo = mock(LabRepository.class);
        when(mockRepo.findById(2L)).thenReturn(Optional.of(existing));
        when(mockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LabServiceImpl service = new LabServiceImpl();
        injectField(service, "labRepository", mockRepo);

        Lab update = new Lab();
        update.setId(2L);
        update.setCollegeId(null);
        service.updateById(update);

        assertEquals(1L, existing.getCollegeId(),
            "collegeId 为 null 时不应覆盖已有值");
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