package com.labcourse.service;

import com.labcourse.entity.Course;
import com.labcourse.entity.Student;
import com.labcourse.repository.AttendanceRepository;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.SelectionRepository;
import com.labcourse.repository.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 存储过程集成测试
 * 验证 proc_check_attendance_status 和 proc_check_course_selection_conflict
 * 在真实数据库环境下的行为
 */
@SpringBootTest
@Transactional
@Sql(scripts = "classpath:sql/prepare_test_data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:sql/cleanup_test_data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SuppressWarnings("null")
class StoredProcedureIntegrationTest {

    private static final ZoneId TEST_ZONE = ZoneId.systemDefault();
    private static final LocalDateTime CHECK_IN_TIME = LocalDateTime.of(2026, 6, 22, 7, 55);
    private static final LocalDate CHECK_IN_DATE = CHECK_IN_TIME.toLocalDate();

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private SelectionService selectionService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private SelectionRepository selectionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @AfterEach
    void resetAttendanceClock() throws Exception {
        setAttendanceClock(Clock.systemDefaultZone());
    }

    /**
     * 将课程时间设置为今天，确保签到测试中课程时间匹配
     */
    private void setCourseTimeForDate(Long courseId, LocalDate date) {
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        int d = date.getDayOfWeek().getValue();
        Course course = courseRepository.findById(courseId).orElseThrow();
        course.setCourseTime(dayNames[d - 1] + " 1-2节");
        courseRepository.save(course);
    }

    private void useFixedCheckInClock() throws Exception {
        setAttendanceClock(Clock.fixed(CHECK_IN_TIME.atZone(TEST_ZONE).toInstant(), TEST_ZONE));
    }

    private void setAttendanceClock(Clock clock) throws Exception {
        Object target = AopTestUtils.getTargetObject(attendanceService);
        ReflectionTestUtils.setField(target, "clock", clock);
    }

    // ================================================================
    // Test 1: 签到成功场景
    // ================================================================

    @Test
    @DisplayName("签到成功：学生签到后返回 success=true 且创建 attendance 记录")
    void testCheckInSuccess() throws Exception {
        useFixedCheckInClock();
        setCourseTimeForDate(9001L, CHECK_IN_DATE);

        Map<String, Object> result = attendanceService.checkIn(9001L, 9001L);

        assertTrue((Boolean) result.get("success"), "签到应成功");
        assertNotNull(result.get("status"), "返回结果应包含状态");
        assertEquals("测试课程A", result.get("courseName"));
        assertEquals("测试学生1", result.get("studentName"));

        // 验证数据库中存在签到记录
        boolean exists = attendanceRepository.existsByStudentIdAndCourseIdAndAttendanceDate(
                9001L, 9001L, CHECK_IN_DATE);
        assertTrue(exists, "数据库中应存在签到记录");
    }

    // ================================================================
    // Test 2: 重复签到场景
    // ================================================================

    @Test
    @DisplayName("重复签到：同一学生同一天对同一课程签到两次，第二次应返回失败")
    void testCheckInDuplicate() throws Exception {
        useFixedCheckInClock();
        setCourseTimeForDate(9001L, CHECK_IN_DATE);

        // 第一次签到
        Map<String, Object> firstResult = attendanceService.checkIn(9001L, 9001L);
        assertTrue((Boolean) firstResult.get("success"), "第一次签到应成功");

        // 第二次签到（重复）
        Map<String, Object> secondResult = attendanceService.checkIn(9001L, 9001L);
        assertFalse((Boolean) secondResult.get("success"), "重复签到应失败");
        assertTrue(((String) secondResult.get("message")).contains("今日已签到"),
                "错误消息应包含'今日已签到'");

        // 验证数据库中只有一条签到记录
        long count = attendanceRepository.findByStudentIdOrderByAttendanceDateDesc(9001L)
                .stream()
                .filter(a -> a.getCourseId().equals(9001L) && a.getAttendanceDate().equals(CHECK_IN_DATE))
                .count();
        assertEquals(1, count, "同一天同一课程应只有一条签到记录");
    }

    // ================================================================
    // Test 3: 选课成功场景
    // ================================================================

    @Test
    @DisplayName("选课成功：学生正常选课应返回 true 并创建 selection 记录")
    void testAddSelectionSuccess() {
        boolean result = selectionService.addSelection(9003L, 9003L);

        assertTrue(result, "选课应成功");

        // 验证数据库中存在选课记录
        boolean exists = selectionRepository.findByStudentIdAndCourseId(9003L, 9003L).isPresent();
        assertTrue(exists, "数据库中应存在选课记录");
    }

    // ================================================================
    // Test 4: 选课容量满场景
    // ================================================================

    @Test
    @DisplayName("选课容量满：课程容量为2，前两个学生选课成功，第三个学生选课失败")
    void testAddSelectionCourseFull() {
        // 前两个学生选课成功（课程9001容量为2）
        boolean first = selectionService.addSelection(9001L, 9001L);
        boolean second = selectionService.addSelection(9002L, 9001L);
        assertTrue(first, "第一个学生选课应成功");
        assertTrue(second, "第二个学生选课应成功");

        // 第三个学生选课失败（容量已满）
        boolean third = selectionService.addSelection(9003L, 9001L);
        assertFalse(third, "第三个学生选课应因容量满而失败");

        // 验证数据库中只有2条选课记录
        long count = selectionRepository.countByCourseId(9001L);
        assertEquals(2, count, "课程9001应只有2条选课记录");
    }

    // ================================================================
    // Test 5: 选课重复场景
    // ================================================================

    @Test
    @DisplayName("选课重复：重复选课应返回 false")
    void testAddSelectionDuplicate() {
        // 第一次选课
        boolean first = selectionService.addSelection(9001L, 9003L);
        assertTrue(first, "第一次选课应成功");

        // 第二次选同一课程
        boolean second = selectionService.addSelection(9001L, 9003L);
        assertFalse(second, "重复选课应失败");

        // 验证数据库中只有1条选课记录
        long count = selectionRepository.findByStudentId(9001L)
                .stream()
                .filter(s -> s.getCourseId().equals(9003L))
                .count();
        assertEquals(1, count, "学生9001对课程9003应只有1条选课记录");
    }

    // ================================================================
    // Test 6: 并发选课锁机制
    // ================================================================

    @Test
    @DisplayName("并发选课锁机制：10个线程并发选课，容量为2的课程最终只有2个选课成功")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testConcurrentCourseSelection() throws Exception {
        selectionRepository.findByCourseId(9001L)
                .forEach(s -> selectionRepository.deleteById(s.getId()));
        selectionRepository.flush();
        Course concurrentCourse = courseRepository.findById(9001L).orElseThrow();
        concurrentCourse.setCollegeId(9001L);
        concurrentCourse.setCourseType("ELECTIVE");
        concurrentCourse.setMaxCount(2);
        courseRepository.saveAndFlush(concurrentCourse);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 学生ID范围：9001-9010
        for (int i = 0; i < threadCount; i++) {
            final long studentId = 9001L + i;
            executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // 所有线程同时出发

                    boolean result = selectionService.addSelection(studentId, 9001L);
                    if (result) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        boolean terminated = executor.awaitTermination(30, TimeUnit.SECONDS);
        assertTrue(terminated, "所有线程应在30秒内完成");

        // 课程9001容量为2，应只有2个选课成功
        assertEquals(2, successCount.get(), "并发选课：应只有2个线程选课成功");
        assertEquals(threadCount - 2, failCount.get(), "并发选课：应有" + (threadCount - 2) + "个线程选课失败");

        // 手动清理并发测试产生的选课记录
        selectionRepository.findByCourseId(9001L)
                .forEach(s -> selectionRepository.deleteById(s.getId()));
    }

    // ================================================================
    // Test 7: 删除过渡字段后关联查询
    // ================================================================

    @Test
    @DisplayName("关联查询：学生 collegeId 和 majorId 外键字段可正常查询")
    void testCollegeAndMajorQuery() {
        // 查询学生
        Student student = studentRepository.findById(9001L).orElseThrow();
        assertNotNull(student, "学生9001应存在");

        // 验证 collegeId 和 majorId 为有效值
        assertEquals(9001L, student.getCollegeId(), "collegeId 应为 9001");
        assertEquals(9001L, student.getMajorId(), "majorId 应为 9001");

        // 查询课程
        Course course = courseRepository.findById(9001L).orElseThrow();
        assertNotNull(course, "课程9001应存在");
        assertEquals(9001L, course.getCollegeId(), "课程 collegeId 应为 9001");

        // 验证通过 collegeId 可以关联到学院名称
        // 此处仅验证 ID 字段存在且有效，实际关联查询由业务层处理
        assertNotNull(student.getCollegeId(), "collegeId 不应为 null");
        assertNotNull(student.getMajorId(), "majorId 不应为 null");
    }
}
