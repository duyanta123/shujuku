package com.labcourse;

import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.SelectionRepository;
import com.labcourse.repository.StudentRepository;
import com.labcourse.service.SelectionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 选课容量并发测试
 * 验证 SelectionService.addSelection 在多线程并发下不会超过课程 max_count
 * 防止 TOCTOU 竞态条件：count 与 insert 之间无锁保护会导致超选
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SelectionCapacityConcurrencyTest {

    @Autowired
    private SelectionService selectionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int THREAD_COUNT = 20;
    private static final int COURSE_MAX_COUNT = 5;

    private static Long testCourseId;
    private static final List<Long> testStudentIds = new ArrayList<>();
    private static Long testCollegeId;

    @BeforeAll
    static void setUp(@Autowired JdbcTemplate jdbcTemplate) {
        Integer collegeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM college", Integer.class);
        if (collegeCount == 0) {
            jdbcTemplate.update("INSERT INTO college (name, status) VALUES ('选课并发测试学院', 'ACTIVE')");
        }
        testCollegeId = jdbcTemplate.queryForObject(
                "SELECT id FROM college WHERE name = '选课并发测试学院'", Long.class);
        if (testCollegeId == null) {
            testCollegeId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM college", Long.class);
        }

        Integer teacherCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM teacher WHERE teacher_no = 'T001'", Integer.class);
        if (teacherCount == 0) {
            jdbcTemplate.update(
                    "INSERT INTO teacher (teacher_no, name, password, college_id) VALUES ('T001', '测试教师', '$2a$10$xho2DUDIw9hwjxC2e7NPvej93757fkeQJHEGTior0Wt.ViGBRphNq', ?)",
                    testCollegeId);
        }
        Long teacherId = jdbcTemplate.queryForObject(
                "SELECT id FROM teacher WHERE teacher_no = 'T001'", Long.class);

        String courseName = "选课并发测试课程_" + System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO course (course_name, teacher_id, max_count, college_id, course_type) VALUES (?, ?, ?, ?, ?)",
                courseName, teacherId, COURSE_MAX_COUNT, testCollegeId, "ELECTIVE");
        testCourseId = jdbcTemplate.queryForObject(
                "SELECT id FROM course WHERE course_name = ?", Long.class, courseName);

        for (int i = 0; i < THREAD_COUNT; i++) {
            String studentNo = "CONCURRENT_SEL_" + System.currentTimeMillis() + "_" + i;
            jdbcTemplate.update(
                    "INSERT INTO student (student_no, name, password, college_id) VALUES (?, ?, ?, ?)",
                    studentNo, "并发学生" + i,
                    "$2a$10$xho2DUDIw9hwjxC2e7NPvej93757fkeQJHEGTior0Wt.ViGBRphNq",
                    testCollegeId);
            Long sid = jdbcTemplate.queryForObject(
                    "SELECT id FROM student WHERE student_no = ?", Long.class, studentNo);
            testStudentIds.add(sid);
        }
    }

    @AfterAll
    static void tearDown(@Autowired JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM selection WHERE course_id = ?", testCourseId);
        if (!testStudentIds.isEmpty()) {
            jdbcTemplate.update("DELETE FROM student WHERE id IN (" +
                    String.join(",", testStudentIds.stream().map(String::valueOf).toArray(String[]::new)) +
                    ")");
        }
        jdbcTemplate.update("DELETE FROM course WHERE id = ?", testCourseId);
    }

    @Test
    @Order(1)
    @DisplayName("并发选课：多线程同时选修容量5的课程，最终成功数应不超过5")
    void concurrentAddSelection_NotExceedMaxCount() throws Exception {
        // 清理之前的选课数据
        jdbcTemplate.update("DELETE FROM selection WHERE course_id = ?", testCourseId);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch readyGate = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (Long sid : testStudentIds) {
            futures.add(executor.submit(() -> {
                try {
                    readyGate.countDown();
                    startGate.await();
                    boolean ok = selectionService.addSelection(sid, testCourseId);
                    if (ok) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }));
        }

        readyGate.await(5, TimeUnit.SECONDS);
        startGate.countDown();

        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        Integer dbCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM selection WHERE course_id = ?", Integer.class, testCourseId);

        System.out.println("并发选课结果：成功=" + successCount.get()
                + ", 失败=" + failCount.get()
                + ", 数据库记录数=" + dbCount
                + ", 课程上限=" + COURSE_MAX_COUNT);

        assertEquals(COURSE_MAX_COUNT, successCount.get(),
                "并发选课：成功数应严格等于课程容量 " + COURSE_MAX_COUNT);
        assertEquals(COURSE_MAX_COUNT, dbCount.intValue(),
                "并发选课：数据库中的选课记录数应严格等于课程容量 " + COURSE_MAX_COUNT);
    }
}
