package com.labcourse.controller;

import com.labcourse.entity.Course;
import com.labcourse.service.CourseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course")
public class CourseController {

    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

    @Autowired
    private CourseService courseService;

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> courses = courseService.getCourseList();
        result.put("success", true);
        result.put("data", courses);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> myCourses() {
        Map<String, Object> result = new HashMap<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!hasRole(authentication, "teacher")) {
            result.put("success", false);
            result.put("message", "仅教师可查看授课课程");
            return ResponseEntity.status(403).body(result);
        }
        Long teacherId = Long.valueOf(authentication.getPrincipal().toString());
        result.put("success", true);
        result.put("data", courseService.getCourseListByTeacherId(teacherId));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list/simple")
    public ResponseEntity<Map<String, Object>> listSimple(@RequestParam(required = false) Long collegeId) {
        Map<String, Object> result = new HashMap<>();
        if (collegeId != null && collegeId <= 0) {
            result.put("success", true);
            result.put("data", java.util.Collections.emptyList());
            return ResponseEntity.ok(result);
        }
        result.put("success", true);
        result.put("data", courseService.list(collegeId));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@Valid @RequestBody Course course) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = courseService.save(course);
            result.put("success", success);
            result.put("message", success ? "添加成功" : "添加失败");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Course course) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = courseService.updateById(course);
            result.put("success", success);
            result.put("message", success ? "更新成功" : "更新失败");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = courseService.removeById(id);
            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败，课程不存在或存在关联数据无法删除");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("删除课程 {} 时发生异常", id, e);
            result.put("success", false);
            result.put("message", "删除失败，请确认该课程下无关联数据或联系管理员");
            return ResponseEntity.internalServerError().body(result);
        }
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
