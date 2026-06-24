package com.labcourse.controller;

import com.labcourse.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String avatarUrl = userService.saveAvatar(file);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            Map<String, Object> data = new HashMap<>();
            data.put("avatarUrl", avatarUrl);
            result.put("data", data);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            logger.warn("Upload avatar failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "头像上传失败"));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile() {
        try {
            Map<String, Object> profile = userService.getProfile();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", profile);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            logger.warn("Get profile failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "获取用户信息失败"));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> data) {
        Map<String, Object> result = new HashMap<>();
        String oldPassword = data.get("oldPassword");
        String newPassword = data.get("newPassword");

        if (oldPassword == null || newPassword == null || oldPassword.isEmpty() || newPassword.isEmpty()) {
            result.put("success", false);
            result.put("message", "旧密码和新密码不能为空");
            return ResponseEntity.ok(result);
        }

        try {
            boolean success = userService.changePassword(oldPassword, newPassword);
            if (success) {
                result.put("success", true);
                result.put("message", "密码修改成功");
            } else {
                result.put("success", false);
                result.put("message", "旧密码错误");
            }
        } catch (RuntimeException e) {
            logger.warn("Change password failed: {}", e.getMessage());
            result.put("success", false);
            result.put("message", e instanceof IllegalArgumentException ? e.getMessage() : "密码修改失败");
        }
        return ResponseEntity.ok(result);
    }
}
