package com.labcourse;

import com.labcourse.entity.*;
import com.labcourse.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库唯一索引冲突测试
 * 验证 UNIQUE 约束在数据库层面阻止重复数据插入
 * 课程考点：唯一约束、数据完整性
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseUniqueIndexTest {

    @Autowired private CollegeRepository collegeRepository;
    @Autowired private MajorRepository majorRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private SelectionRepository selectionRepository;
    @Autowired private AttendanceRepository attendanceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Test 1: 重复学号插入 → 被拒绝
     * 种子数据中已存在 studentNo='S001'
     */
    @Test
    @Order(1)
    @Transactional
    @DisplayName("唯一约束：重复学号插入应被拒绝")
    void duplicateStudentNo_ShouldBeRejected() {
        Student duplicate = new Student();
        duplicate.setStudentNo("S001");
        duplicate.setName("测试重复学号");
        duplicate.setPassword("password123");

        assertThrows(Exception.class, () -> {
            studentRepository.save(duplicate);
            entityManager.flush();
        }, "插入重复学号应该抛出唯一约束异常");
    }

    /**
     * Test 2: 重复工号插入 → 被拒绝
     * 种子数据中已存在 teacherNo='T001'
     */
    @Test
    @Order(2)
    @Transactional
    @DisplayName("唯一约束：重复工号插入应被拒绝")
    void duplicateTeacherNo_ShouldBeRejected() {
        Teacher duplicate = new Teacher();
        duplicate.setTeacherNo("T001");
        duplicate.setName("测试重复工号");
        duplicate.setPassword("password123");

        assertThrows(Exception.class, () -> {
            teacherRepository.save(duplicate);
            entityManager.flush();
        }, "插入重复工号应该抛出唯一约束异常");
    }

    /**
     * Test 3: 重复用户名插入 → 被拒绝
     * 种子数据中已存在 username='admin'
     */
    @Test
    @Order(3)
    @Transactional
    @DisplayName("唯一约束：重复用户名插入应被拒绝")
    void duplicateUsername_ShouldBeRejected() {
        Admin duplicate = new Admin();
        duplicate.setUsername("admin");
        duplicate.setPassword("password123");

        assertThrows(Exception.class, () -> {
            adminRepository.save(duplicate);
            entityManager.flush();
        }, "插入重复用户名应该抛出唯一约束异常");
    }

    /**
     * Test 4: 重复学院名插入 → 被拒绝
     * 利用种子数据中已存在的学院名称构造冲突
     */
    @Test
    @Order(4)
    @Transactional
    @DisplayName("唯一约束：重复学院名插入应被拒绝")
    void duplicateCollegeName_ShouldBeRejected() {
        College existing = collegeRepository.findById(1L).orElse(null);
        Assumptions.assumeTrue(existing != null, "种子数据中应存在 college_id=1");

        College duplicate = new College();
        duplicate.setName(existing.getName());
        duplicate.setStatus("ACTIVE");

        assertThrows(Exception.class, () -> {
            collegeRepository.save(duplicate);
            entityManager.flush();
        }, "插入重复学院名应该抛出唯一约束异常");
    }

    /**
     * Test 5: 同学院下重复专业名插入 → 被拒绝
     * 利用种子数据中已存在的专业名称构造冲突
     */
    @Test
    @Order(5)
    @Transactional
    @DisplayName("唯一约束：同学院下重复专业名插入应被拒绝")
    void duplicateMajorNameInSameCollege_ShouldBeRejected() {
        Major existing = majorRepository.findById(1L).orElse(null);
        Assumptions.assumeTrue(existing != null, "种子数据中应存在 major_id=1");

        Major duplicate = new Major();
        duplicate.setName(existing.getName());
        duplicate.setCollegeId(existing.getCollegeId());
        duplicate.setStatus("ACTIVE");

        assertThrows(Exception.class, () -> {
            majorRepository.save(duplicate);
            entityManager.flush();
        }, "同学院下插入重复专业名应该抛出唯一约束异常");
    }

    /**
     * Test 6: 同学生同课程重复选课 → 被拒绝
     * 先创建一条选课记录，再尝试插入重复记录触发唯一约束
     */
    @Test
    @Order(6)
    @Transactional
    @DisplayName("唯一约束：同学生同课程重复选课应被拒绝")
    void duplicateSelection_ShouldBeRejected() {
        // 先创建一条选课记录（利用种子数据中的学生和课程）
        Selection first = new Selection();
        first.setStudentId(2L);
        first.setCourseId(1L);
        selectionRepository.save(first);
        entityManager.flush();

        // 尝试重复选课
        Selection duplicate = new Selection();
        duplicate.setStudentId(2L);
        duplicate.setCourseId(1L);

        assertThrows(Exception.class, () -> {
            selectionRepository.save(duplicate);
            entityManager.flush();
        }, "重复选课应该抛出唯一约束异常");
    }

    /**
     * Test 7: 同学生同课程同天重复签到 → 被拒绝
     * 先创建一条签到记录，再尝试重复签到触发唯一约束
     */
    @Test
    @Order(7)
    @Transactional
    @DisplayName("唯一约束：同学生同课程同天重复签到应被拒绝")
    void duplicateAttendance_ShouldBeRejected() {
        LocalDate today = LocalDate.now();

        // 先创建一条签到记录
        Attendance first = new Attendance();
        first.setStudentId(2L);
        first.setCourseId(1L);
        first.setAttendanceStatus(AttendanceStatus.出勤);
        first.setAttendanceDate(today);
        first.setCheckInTime(java.time.LocalDateTime.now());
        attendanceRepository.save(first);
        entityManager.flush();

        // 尝试重复签到
        Attendance duplicate = new Attendance();
        duplicate.setStudentId(2L);
        duplicate.setCourseId(1L);
        duplicate.setAttendanceStatus(AttendanceStatus.出勤);
        duplicate.setAttendanceDate(today);
        duplicate.setCheckInTime(java.time.LocalDateTime.now());

        assertThrows(Exception.class, () -> {
            attendanceRepository.save(duplicate);
            entityManager.flush();
        }, "同一天重复签到应该抛出唯一约束异常");
    }
}