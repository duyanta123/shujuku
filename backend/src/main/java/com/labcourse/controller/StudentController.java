package com.labcourse.controller;

import com.labcourse.entity.Student;
import com.labcourse.repository.StudentRepository;
import com.labcourse.service.StudentService;
import com.labcourse.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginData) {
        String studentNo = loginData.get("studentNo");
        String password = loginData.get("password");

        Student student = studentService.login(studentNo, password);
        Map<String, Object> result = new HashMap<>();
        if (student != null) {
            String accessToken = jwtUtil.generateAccessToken(student.getId(), student.getStudentNo(), "student");
            String refreshToken = jwtUtil.generateRefreshToken(student.getId());
            student.setRefreshToken(refreshToken);
            studentRepository.save(student);
            result.put("success", true);
            result.put("message", "登录成功");
            result.put("data", Map.of(
                    "id", student.getId(),
                    "studentNo", student.getStudentNo(),
                    "name", student.getName(),
                    "gender", student.getGender(),
                    "collegeId", student.getCollegeId(),
                    "majorId", student.getMajorId(),
                    "avatarUrl", student.getAvatarUrl()
            ));
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            return ResponseEntity.ok(result);
        } else {
            result.put("success", false);
            result.put("message", "学号或密码错误");
            return ResponseEntity.status(401).body(result);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) Long collegeId) {
        Map<String, Object> result = new HashMap<>();
        // collegeId 非法值（<=0）返回空列表
        if (collegeId != null && collegeId <= 0) {
            result.put("success", true);
            result.put("data", java.util.Collections.emptyList());
            return ResponseEntity.ok(result);
        }
        result.put("success", true);
        result.put("data", studentService.list(collegeId));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@Valid @RequestBody Student student) {
        Map<String, Object> result = new HashMap<>();
        boolean success = studentService.save(student);
        result.put("success", success);
        result.put("message", success ? "添加成功" : "添加失败");
        return ResponseEntity.ok(result);
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Student student) {
        Map<String, Object> result = new HashMap<>();
        boolean success = studentService.updateById(student);
        result.put("success", success);
        result.put("message", success ? "更新成功" : "更新失败");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        boolean success = studentService.removeById(id);
        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败");
        return ResponseEntity.ok(result);
    }

    // 管理员重置学生密码为随机8位密码
    @PostMapping("/reset-password/{id}")
    public ResponseEntity<Map<String, Object>> resetPassword(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        String newPassword = studentService.resetPassword(id);
        if (newPassword != null) {
            result.put("success", true);
            result.put("message", "密码重置成功");
            result.put("data", Map.of("temporaryPassword", newPassword));
        } else {
            result.put("success", false);
            result.put("message", "重置失败，学生不存在");
        }
        return ResponseEntity.ok(result);
    }

    // 学生自助修改密码（从JWT中获取当前用户ID）
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

        boolean success = studentService.changePassword(userId, oldPassword, newPassword);
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
