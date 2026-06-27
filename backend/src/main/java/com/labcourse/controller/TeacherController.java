package com.labcourse.controller;

import com.labcourse.entity.Teacher;
import com.labcourse.repository.TeacherRepository;
import com.labcourse.service.TeacherService;
import com.labcourse.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        String teacherNo = loginData.get("teacherNo");
        String password = loginData.get("password");

        Teacher teacher = teacherService.login(teacherNo, password);
        Map<String, Object> result = new HashMap<>();

        if (teacher != null) {
            String accessToken = jwtUtil.generateAccessToken(teacher.getId(), teacher.getTeacherNo(), "teacher");
            String refreshToken = jwtUtil.generateRefreshToken(teacher.getId());
            teacher.setRefreshToken(refreshToken);
            teacherRepository.save(teacher);
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("data", Map.of(
                    "id", teacher.getId(),
                    "teacherNo", teacher.getTeacherNo(),
                    "name", teacher.getName(),
                    "title", teacher.getTitle(),
                    "collegeId", teacher.getCollegeId(),
                    "avatarUrl", teacher.getAvatarUrl()
            ));
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            return ResponseEntity.ok(result);
        } else {
            result.put("success", false);
            result.put("message", "工号或密码错误");
            return ResponseEntity.status(401).body(result);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) Long collegeId) {
        Map<String, Object> result = new HashMap<>();
        if (collegeId != null && collegeId <= 0) {
            result.put("success", true);
            result.put("data", java.util.Collections.emptyList());
            return ResponseEntity.ok(result);
        }
        result.put("success", true);
        result.put("data", teacherService.list(collegeId));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@Valid @RequestBody Teacher teacher) {
        Map<String, Object> result = new HashMap<>();
        boolean success = teacherService.save(teacher);
        result.put("success", success);
        result.put("message", success ? "添加成功" : "添加失败");
        return ResponseEntity.ok(result);
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Teacher teacher) {
        Map<String, Object> result = new HashMap<>();
        boolean success = teacherService.updateById(teacher);
        result.put("success", success);
        result.put("message", success ? "更新成功" : "更新失败");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        boolean success = teacherService.removeById(id);
        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败");
        return ResponseEntity.ok(result);
    }

    // 管理员重置教师密码为随机8位密码
    @PostMapping("/reset-password/{id}")
    public ResponseEntity<Map<String, Object>> resetPassword(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        String newPassword = teacherService.resetPassword(id);
        if (newPassword != null) {
            result.put("success", true);
            result.put("message", "密码重置成功");
            result.put("data", Map.of("temporaryPassword", newPassword));
        } else {
            result.put("success", false);
            result.put("message", "重置失败，教师不存在");
        }
        return ResponseEntity.ok(result);
    }

    // 教师自助修改密码（从JWT中获取当前用户ID）
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> data) {
        Map<String, Object> result = new HashMap<>();
        // 从SecurityContext获取当前登录用户ID
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String oldPassword = data.get("oldPassword");
        String newPassword = data.get("newPassword");

        if (oldPassword == null || newPassword == null || oldPassword.isEmpty() || newPassword.isEmpty()) {
            result.put("success", false);
            result.put("message", "旧密码和新密码不能为空");
            return ResponseEntity.ok(result);
        }

        boolean success = teacherService.changePassword(userId, oldPassword, newPassword);
        if (success) {
            result.put("success", true);
            result.put("message", "密码修改成功");
        } else {
            result.put("success", false);
            result.put("message", "旧密码错误");
        }
        return ResponseEntity.ok(result);
    }
}
