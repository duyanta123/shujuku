package com.labcourse.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Profile({"dev", "test"})
public class AdminTestDataController {

    private static final Logger logger = LoggerFactory.getLogger(AdminTestDataController.class);

    private final JdbcTemplate jdbcTemplate;

    public AdminTestDataController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/cleanup-test-data")
    @Transactional
    public ResponseEntity<Map<String, Object>> cleanupTestData() {
        Map<String, Object> result = new HashMap<>();
        int total = 0;

        int n = jdbcTemplate.update("DELETE FROM selection WHERE course_id IN (SELECT id FROM course WHERE course_name LIKE ? OR course_name LIKE ?)", "%测试%", "%test%");
        logger.info("Cleaned test selections by course: {}", n);
        total += n;

        n = jdbcTemplate.update("DELETE FROM score WHERE course_id IN (SELECT id FROM course WHERE course_name LIKE ? OR course_name LIKE ?)", "%测试%", "%test%");
        logger.info("Cleaned test scores by course: {}", n);
        total += n;

        n = jdbcTemplate.update("DELETE FROM attendance WHERE course_id IN (SELECT id FROM course WHERE course_name LIKE ? OR course_name LIKE ?)", "%测试%", "%test%");
        logger.info("Cleaned test attendance by course: {}", n);
        total += n;

        n = jdbcTemplate.update("DELETE FROM selection WHERE student_id IN (SELECT id FROM student WHERE name LIKE ? OR name LIKE ?)", "%测试%", "%test%");
        logger.info("Cleaned test selections by student: {}", n);
        total += n;

        n = jdbcTemplate.update("DELETE FROM score WHERE student_id IN (SELECT id FROM student WHERE name LIKE ? OR name LIKE ?)", "%测试%", "%test%");
        logger.info("Cleaned test scores by student: {}", n);
        total += n;

        n = jdbcTemplate.update("DELETE FROM attendance WHERE student_id IN (SELECT id FROM student WHERE name LIKE ? OR name LIKE ?)", "%测试%", "%test%");
        logger.info("Cleaned test attendance by student: {}", n);
        total += n;

        n = jdbcTemplate.update("DELETE FROM major_required_course WHERE course_id IN (SELECT id FROM course WHERE course_name LIKE ? OR course_name LIKE ?)", "%测试%", "%test%");
        logger.info("Cleaned test major-course links: {}", n);
        total += n;

        n = jdbcTemplate.update("DELETE FROM course WHERE course_name LIKE ? OR course_name LIKE ?", "%测试%", "%test%");
        result.put("courseCleaned", n);
        total += n;

        n = jdbcTemplate.update("DELETE FROM student WHERE name LIKE ? OR name LIKE ?", "%测试%", "%test%");
        result.put("studentCleaned", n);
        total += n;

        n = jdbcTemplate.update("DELETE FROM lab WHERE lab_name LIKE ? OR lab_name LIKE ?", "%测试%", "%test%");
        result.put("labCleaned", n);
        total += n;

        n = jdbcTemplate.update("DELETE FROM teacher WHERE name LIKE ? OR name LIKE ?", "%测试%", "%test%");
        result.put("teacherCleaned", n);
        total += n;

        n = jdbcTemplate.update("UPDATE major SET status = 'INACTIVE' WHERE (name LIKE ? OR name LIKE ?) AND status = 'ACTIVE'", "%测试%", "%test%");
        result.put("majorCleaned", n);
        total += n;

        n = jdbcTemplate.update("UPDATE college SET status = 'INACTIVE' WHERE (name LIKE ? OR name LIKE ?) AND status = 'ACTIVE'", "%测试%", "%test%");
        result.put("collegeCleaned", n);
        total += n;

        result.put("totalRecords", total);
        result.put("success", total > 0);
        if (total == 0) {
            result.put("message", "No test data found");
        }
        return ResponseEntity.ok(result);
    }
}
