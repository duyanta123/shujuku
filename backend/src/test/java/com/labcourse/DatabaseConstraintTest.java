package com.labcourse;

import com.labcourse.entity.*;
import com.labcourse.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库外键约束测试
 * 验证 ON DELETE RESTRICT / 外键约束在数据库层面生效
 * 课程考点：参照完整性、外键约束
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseConstraintTest {

    @Autowired private CollegeRepository collegeRepository;
    @Autowired private MajorRepository majorRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private SelectionRepository selectionRepository;
    @Autowired private ScoreRepository scoreRepository;
    @Autowired private AttendanceRepository attendanceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Test 1: 删除有关联专业的学院 → 被 RDBMS 拒绝
     * 利用种子数据中的 college_id=1（该学院下存在专业）
     */
    @Test
    @Order(1)
    @Transactional
    @DisplayName("外键约束：删除有关联专业的学院应被拒绝")
    void deleteCollegeWithAssociatedMajors_ShouldBeRejected() {
        assertThrows(Exception.class, () -> {
            collegeRepository.deleteById(1L);
            entityManager.flush();
        }, "删除有关联专业的学院应该抛出外键约束异常");
    }

    /**
     * Test 2: 删除有关联学生的专业 → 被拒绝
     * 利用种子数据中 college_id=1 下的 major（该专业下有学生）
     */
    @Test
    @Order(2)
    @Transactional
    @DisplayName("外键约束：删除有关联学生的专业应被拒绝")
    void deleteMajorWithAssociatedStudents_ShouldBeRejected() {
        assertThrows(Exception.class, () -> {
            majorRepository.deleteById(1L);
            entityManager.flush();
        }, "删除有关联学生的专业应该抛出外键约束异常");
    }

    /**
     * Test 3: 删除有选课记录的学生 → 被拒绝
     * 利用种子数据中 student_id=1 (S001)，该生存在选课记录
     */
    @Test
    @Order(3)
    @Transactional
    @DisplayName("外键约束：删除有选课记录的学生应被拒绝")
    void deleteStudentWithSelections_ShouldBeRejected() {
        assertThrows(Exception.class, () -> {
            studentRepository.deleteById(1L);
            entityManager.flush();
        }, "删除有选课记录的学生应该抛出外键约束异常");
    }

    /**
     * Test 4: 删除有成绩记录的学生 → 被拒绝
     * 利用种子数据中 student_id=1 (S001)，该生存在成绩记录
     */
    @Test
    @Order(4)
    @Transactional
    @DisplayName("外键约束：删除有成绩记录的学生应被拒绝")
    void deleteStudentWithScores_ShouldBeRejected() {
        assertThrows(Exception.class, () -> {
            studentRepository.deleteById(1L);
            entityManager.flush();
        }, "删除有成绩记录的学生应该抛出外键约束异常");
    }

    /**
     * Test 5: 删除有考勤记录的学生 → 被拒绝
     * 利用种子数据中 student_id=1 (S001)，该生存在考勤记录
     */
    @Test
    @Order(5)
    @Transactional
    @DisplayName("外键约束：删除有考勤记录的学生应被拒绝")
    void deleteStudentWithAttendance_ShouldBeRejected() {
        assertThrows(Exception.class, () -> {
            studentRepository.deleteById(1L);
            entityManager.flush();
        }, "删除有考勤记录的学生应该抛出外键约束异常");
    }

    /**
     * Test 6: 插入不存在的外键值 → 被拒绝
     * 尝试插入 student_id=999999L 的选课记录，该学生不存在
     */
    @Test
    @Order(6)
    @Transactional
    @DisplayName("外键约束：插入不存在的外键值应被拒绝")
    void insertSelectionWithNonExistentStudent_ShouldBeRejected() {
        Selection selection = new Selection();
        selection.setStudentId(999999L);
        selection.setCourseId(1L);

        assertThrows(Exception.class, () -> {
            selectionRepository.save(selection);
            entityManager.flush();
        }, "插入不存在学生ID的选课记录应该抛出外键约束异常");
    }
}