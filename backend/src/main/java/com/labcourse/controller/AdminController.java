package com.labcourse.controller;

import com.labcourse.entity.Admin;
import com.labcourse.repository.AdminRepository;
import com.labcourse.service.AdminService;
import com.labcourse.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        Admin admin = adminService.login(username, password);
        Map<String, Object> result = new HashMap<>();

        if (admin != null) {
            // Security fix (HIGH-001): 生成双Token
            String accessToken = jwtUtil.generateAccessToken(admin.getId(), admin.getUsername(), "admin");
            String refreshToken = jwtUtil.generateRefreshToken(admin.getId());
            admin.setRefreshToken(refreshToken);
            adminRepository.save(admin);
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("data", admin);
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            return ResponseEntity.ok(result);
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
            return ResponseEntity.status(401).body(result);
        }
    }

    @PostMapping("/cleanup-test-data")
    @Transactional
    public ResponseEntity<Map<String, Object>> cleanupTestData() {
        Map<String, Object> result = new HashMap<>();
        if (java.util.Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            result.put("success", false);
            result.put("message", "该接口不允许在生产环境使用");
            return ResponseEntity.status(403).body(result);
        }
        int total = 0;

        // 1. 删除测试课程的选课记录
        int n = jdbcTemplate.update("DELETE FROM selection WHERE course_id IN (SELECT id FROM course WHERE course_name LIKE ? OR course_name LIKE ?)", "%测试%", "%test%");
        logger.info("清理测试数据 - 选课记录: {} 条", n); total += n;

        // 2. 删除测试课程的成绩记录
        n = jdbcTemplate.update("DELETE FROM score WHERE course_id IN (SELECT id FROM course WHERE course_name LIKE ? OR course_name LIKE ?)", "%测试%", "%test%");
        logger.info("清理测试数据 - 成绩记录: {} 条", n); total += n;

        // 3. 删除测试课程的考勤记录
        n = jdbcTemplate.update("DELETE FROM attendance WHERE course_id IN (SELECT id FROM course WHERE course_name LIKE ? OR course_name LIKE ?)", "%测试%", "%test%");
        logger.info("清理测试数据 - 考勤记录: {} 条", n); total += n;

        // 4. 删除测试学生的选课/成绩/考勤
        n = jdbcTemplate.update("DELETE FROM selection WHERE student_id IN (SELECT id FROM student WHERE name LIKE ? OR name LIKE ?)", "%测试%", "%test%");
        logger.info("清理测试数据 - 测试学生选课: {} 条", n); total += n;
        n = jdbcTemplate.update("DELETE FROM score WHERE student_id IN (SELECT id FROM student WHERE name LIKE ? OR name LIKE ?)", "%测试%", "%test%");
        logger.info("清理测试数据 - 测试学生成绩: {} 条", n); total += n;
        n = jdbcTemplate.update("DELETE FROM attendance WHERE student_id IN (SELECT id FROM student WHERE name LIKE ? OR name LIKE ?)", "%测试%", "%test%");
        logger.info("清理测试数据 - 测试学生考勤: {} 条", n); total += n;

        // 5. 删除测试课程的 major_required_course 关联
        n = jdbcTemplate.update("DELETE FROM major_required_course WHERE course_id IN (SELECT id FROM course WHERE course_name LIKE ? OR course_name LIKE ?)", "%测试%", "%test%");
        logger.info("清理测试数据 - 课程-专业关联: {} 条", n); total += n;

        // 6. 删除测试课程
        n = jdbcTemplate.update("DELETE FROM course WHERE course_name LIKE ? OR course_name LIKE ?", "%测试%", "%test%");
        result.put("courseCleaned", n);
        logger.info("清理测试数据 - 课程: {} 个", n); total += n;

        // 7. 删除测试学生
        n = jdbcTemplate.update("DELETE FROM student WHERE name LIKE ? OR name LIKE ?", "%测试%", "%test%");
        result.put("studentCleaned", n);
        logger.info("清理测试数据 - 学生: {} 个", n); total += n;

        // 8. 删除测试实验室
        n = jdbcTemplate.update("DELETE FROM lab WHERE lab_name LIKE ? OR lab_name LIKE ?", "%测试%", "%test%");
        result.put("labCleaned", n);
        logger.info("清理测试数据 - 实验室: {} 个", n); total += n;

        // 9. 删除测试教师
        n = jdbcTemplate.update("DELETE FROM teacher WHERE name LIKE ? OR name LIKE ?", "%测试%", "%test%");
        result.put("teacherCleaned", n);
        logger.info("清理测试数据 - 教师: {} 个", n); total += n;

        // 10. 软删除测试专业
        n = jdbcTemplate.update("UPDATE major SET status = 'INACTIVE' WHERE (name LIKE ? OR name LIKE ?) AND status = 'ACTIVE'", "%测试%", "%test%");
        result.put("majorCleaned", n);
        logger.info("清理测试数据 - 专业: {} 个", n); total += n;

        // 11. 软删除测试学院
        n = jdbcTemplate.update("UPDATE college SET status = 'INACTIVE' WHERE (name LIKE ? OR name LIKE ?) AND status = 'ACTIVE'", "%测试%", "%test%");
        result.put("collegeCleaned", n);
        logger.info("清理测试数据 - 学院: {} 个", n); total += n;

        result.put("totalRecords", total);
        if (total == 0) {
            result.put("success", false);
            result.put("message", "No test data found");
        } else {
            result.put("success", true);
        }
        return ResponseEntity.ok(result);
    }
}
