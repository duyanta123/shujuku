package com.labcourse.service.impl;

import com.labcourse.entity.Admin;
import com.labcourse.entity.Student;
import com.labcourse.entity.Teacher;
import com.labcourse.repository.AdminRepository;
import com.labcourse.repository.StudentRepository;
import com.labcourse.repository.TeacherRepository;
import com.labcourse.service.UserService;
import com.labcourse.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String AVATAR_URL_PREFIX = "/api/static/avatars/";
    private static final int AVATAR_SIZE = 200;

    @Value("${app.upload.avatar-dir:d:/789/backend/uploads/avatars/}")
    private String avatarDir;

    @Override
    @Transactional
    public String saveAvatar(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || !List.of("image/jpeg", "image/png", "image/webp").contains(contentType.toLowerCase())) {
            throw new RuntimeException("仅支持 JPG、PNG、WebP 格式");
        }

        // Validate file size (max 2MB)
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new RuntimeException("图片大小不能超过 2MB");
        }

        String token = extractToken();
        Long userId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        if (userId == null || role == null) {
            throw new RuntimeException("无效的Token");
        }

        // Validate image
        BufferedImage originalImage;
        try {
            originalImage = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("无法读取图片文件");
        }
        if (originalImage == null) {
            throw new RuntimeException("无法解析图片文件");
        }

        // Crop to square (center crop)
        BufferedImage croppedImage = cropToSquare(originalImage);

        // Resize to 200x200
        BufferedImage resizedImage = resizeImage(croppedImage, AVATAR_SIZE, AVATAR_SIZE);

        // Save to disk
        File dir = new File(avatarDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filename = java.util.UUID.randomUUID().toString() + ".png";
        File destFile = new File(dir, filename);
        try {
            ImageIO.write(resizedImage, "png", destFile);
        } catch (IOException e) {
            throw new RuntimeException("保存头像文件失败");
        }

        // Delete old avatar file before updating DB
        String oldAvatarUrl = getCurrentUserAvatar(userId, role);
        deleteOldAvatar(oldAvatarUrl);

        String avatarUrl = AVATAR_URL_PREFIX + filename;

        // Update entity avatarUrl
        try {
            updateUserAvatar(userId, role, avatarUrl);
        } catch (Exception e) {
            // Rollback: delete the newly saved file if DB update fails
            if (destFile.exists()) {
                destFile.delete();
            }
            throw new RuntimeException("更新头像信息失败");
        }

        return avatarUrl;
    }

    @Override
    public Map<String, Object> getProfile() {
        String token = extractToken();
        Long userId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        if (userId == null || role == null) {
            throw new RuntimeException("无效的Token");
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("role", role);

        switch (role.toLowerCase()) {
            case "student" -> {
                Student student = studentRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("学生不存在"));
                profile.put("name", student.getName());
                profile.put("account", student.getStudentNo());
                profile.put("avatarUrl", student.getAvatarUrl());
                profile.put("collegeId", student.getCollegeId());
            }
            case "teacher" -> {
                Teacher teacher = teacherRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("教师不存在"));
                profile.put("name", teacher.getName());
                profile.put("account", teacher.getTeacherNo());
                profile.put("avatarUrl", teacher.getAvatarUrl());
                profile.put("collegeId", teacher.getCollegeId());
                profile.put("title", teacher.getTitle());
            }
            case "admin" -> {
                Admin admin = adminRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("管理员不存在"));
                profile.put("name", admin.getUsername());
                profile.put("account", admin.getUsername());
                profile.put("avatarUrl", admin.getAvatarUrl());
                profile.put("college", "");
            }
            default -> throw new RuntimeException("未知角色: " + role);
        }

        return profile;
    }

    @Override
    public boolean changePassword(String oldPassword, String newPassword) {
        String token = extractToken();
        Long userId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        if (userId == null || role == null) {
            return false;
        }

        return switch (role.toLowerCase()) {
            case "student" -> {
                Student student = studentRepository.findById(userId).orElse(null);
                if (student == null) yield false;
                if (!passwordEncoder.matches(oldPassword, student.getPassword())) yield false;
                student.setPassword(passwordEncoder.encode(newPassword));
                studentRepository.save(student);
                yield true;
            }
            case "teacher" -> {
                Teacher teacher = teacherRepository.findById(userId).orElse(null);
                if (teacher == null) yield false;
                if (!passwordEncoder.matches(oldPassword, teacher.getPassword())) yield false;
                teacher.setPassword(passwordEncoder.encode(newPassword));
                teacherRepository.save(teacher);
                yield true;
            }
            default -> false;
        };
    }

    private String extractToken() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("缺少Authorization头或格式不正确");
        }
        String token = authHeader.substring(7);
        if (jwtUtil.validateAccessToken(token) == null) {
            throw new RuntimeException("Token无效或已过期");
        }
        return token;
    }

    private BufferedImage cropToSquare(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        int side = Math.min(width, height);
        int x = (width - side) / 2;
        int y = (height - side) / 2;
        return original.getSubimage(x, y, side, side);
    }

    private BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        Image scaled = original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    private void updateUserAvatar(Long userId, String role, String avatarUrl) {
        switch (role.toLowerCase()) {
            case "student" -> {
                Student student = studentRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("学生不存在"));
                student.setAvatarUrl(avatarUrl);
                studentRepository.save(student);
            }
            case "teacher" -> {
                Teacher teacher = teacherRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("教师不存在"));
                teacher.setAvatarUrl(avatarUrl);
                teacherRepository.save(teacher);
            }
            case "admin" -> {
                Admin admin = adminRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("管理员不存在"));
                admin.setAvatarUrl(avatarUrl);
                adminRepository.save(admin);
            }
            default -> throw new RuntimeException("未知角色: " + role);
        }
    }

    private String getCurrentUserAvatar(Long userId, String role) {
        switch (role.toLowerCase()) {
            case "student" -> {
                return studentRepository.findById(userId).map(Student::getAvatarUrl).orElse(null);
            }
            case "teacher" -> {
                return teacherRepository.findById(userId).map(Teacher::getAvatarUrl).orElse(null);
            }
            case "admin" -> {
                return adminRepository.findById(userId).map(Admin::getAvatarUrl).orElse(null);
            }
            default -> throw new RuntimeException("未知角色: " + role);
        }
    }

    private void deleteOldAvatar(String oldAvatarUrl) {
        if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty() && oldAvatarUrl.startsWith(AVATAR_URL_PREFIX)) {
            try {
                String filename = oldAvatarUrl.substring(AVATAR_URL_PREFIX.length());
                File oldFile = new File(avatarDir + filename);
                if (oldFile.exists()) {
                    boolean deleted = oldFile.delete();
                    if (!deleted) {
                        // Log but don't throw - old file cleanup is best-effort
                        System.err.println("无法删除旧头像文件: " + oldFile.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                System.err.println("删除旧头像文件时出错: " + e.getMessage());
            }
        }
    }
}
