package com.labcourse.controller;

import com.labcourse.entity.Course;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.SelectionRepository;
import com.labcourse.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/score")
public class ScoreController {

    @Autowired
    private ScoreService scoreService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SelectionRepository selectionRepository;

    @GetMapping("/course/{courseId}")
    public ResponseEntity<Map<String, Object>> courseScores(@PathVariable Long courseId) {
        Map<String, Object> result = new HashMap<>();
        if (courseId == null || courseId <= 0) {
            result.put("success", false);
            result.put("code", "INVALID_PARAMETER");
            result.put("message", "Invalid courseId");
            return ResponseEntity.badRequest().body(result);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            result.put("success", false);
            result.put("code", "COURSE_NOT_FOUND");
            result.put("message", "课程不存在");
            return ResponseEntity.status(404).body(result);
        }

        if (hasRole(authentication, "teacher")) {
            Long currentTeacherId = Long.valueOf(authentication.getPrincipal().toString());
            if (!course.getTeacherId().equals(currentTeacherId)) {
                result.put("success", false);
                result.put("code", "NOT_COURSE_TEACHER");
                result.put("message", "无权查看此课程的成绩");
                return ResponseEntity.status(403).body(result);
            }
        } else if (!hasRole(authentication, "admin")) {
            result.put("success", false);
            result.put("code", "FORBIDDEN");
            result.put("message", "无权查看此课程的成绩");
            return ResponseEntity.status(403).body(result);
        }

        List<Map<String, Object>> records = scoreService.getScoresByCourse(courseId);
        result.put("success", true);
        result.put("data", records);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@RequestBody Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();

        // Security fix (MED-005): 输入验证
        Object studentIdObj = data.get("studentId");
        Object courseIdObj = data.get("courseId");
        Object scoreObj = data.get("score");

        if (studentIdObj == null || courseIdObj == null || scoreObj == null) {
            result.put("success", false);
            result.put("message", "studentId, courseId, score 不能为空");
            return ResponseEntity.badRequest().body(result);
        }

        Long studentId;
        Long courseId;
        BigDecimal score;
        try {
            studentId = Long.valueOf(studentIdObj.toString());
            courseId = Long.valueOf(courseIdObj.toString());
            score = new BigDecimal(scoreObj.toString());
        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("message", "Invalid numeric parameter");
            return ResponseEntity.badRequest().body(result);
        }

        // 成绩值校验：0-100
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            result.put("success", false);
            result.put("message", "成绩必须在0-100之间");
            return ResponseEntity.badRequest().body(result);
        }

        // ID校验：必须为正数
        if (studentId <= 0 || courseId <= 0) {
            result.put("success", false);
            result.put("message", "无效的ID参数");
            return ResponseEntity.badRequest().body(result);
        }

        // Security fix (MEDIUM): 验证当前教师是否是该课程的授课教师
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!hasRole(authentication, "teacher")) {
            result.put("success", false);
            result.put("code", "FORBIDDEN");
            result.put("message", "仅教师可录入成绩");
            return ResponseEntity.status(403).body(result);
        }
        Long currentTeacherId = Long.valueOf(authentication.getPrincipal().toString());
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || !course.getTeacherId().equals(currentTeacherId)) {
            result.put("success", false);
            result.put("code", "NOT_COURSE_TEACHER");
            result.put("message", "无权为此课程的学生录入成绩");
            return ResponseEntity.status(403).body(result);
        }

        // 验证学生是否已选修该课程
        if (selectionRepository.findByStudentIdAndCourseId(studentId, courseId).isEmpty()) {
            result.put("success", false);
            result.put("code", "STUDENT_NOT_SELECTED");
            result.put("message", "该学生未选修此课程，无法录入成绩");
            return ResponseEntity.status(403).body(result);
        }

        boolean success = scoreService.addScore(studentId, courseId, score);

        result.put("success", success);
        result.put("message", success ? "成绩录入成功" : "成绩录入失败");
        return ResponseEntity.ok(result);
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || role == null) {
            return false;
        }
        String expected = role.toLowerCase();
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority != null)
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                .anyMatch(authority -> authority.equalsIgnoreCase(expected));
    }
}
