package com.labcourse.service;

import com.labcourse.entity.Admin;
import com.labcourse.entity.Student;
import com.labcourse.entity.Teacher;
import com.labcourse.repository.AdminRepository;
import com.labcourse.repository.StudentRepository;
import com.labcourse.repository.TeacherRepository;
import com.labcourse.service.impl.UserServiceImpl;
import com.labcourse.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试 — 验证头像上传数据完整性修复
 *
 * 关键缺陷修复验证：
 * - saveAvatar: DB更新失败时旧头像文件不应被删除（防止数据丢失）
 * - saveAvatar: 成功更新后旧头像文件才被删除
 */
@SuppressWarnings("null")
class UserServiceImplTest {

    private UserServiceImpl service;
    private JwtUtil jwtUtil;
    private HttpServletRequest request;
    private StudentRepository studentRepository;
    private TeacherRepository teacherRepository;
    private AdminRepository adminRepository;
    private PasswordEncoder passwordEncoder;

    private static final String AVATAR_DIR = "/tmp/test_avatars/";
    private static final String AVATAR_URL_PREFIX = "/api/static/avatars/";

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl();
        jwtUtil = mock(JwtUtil.class);
        request = mock(HttpServletRequest.class);
        studentRepository = mock(StudentRepository.class);
        teacherRepository = mock(TeacherRepository.class);
        adminRepository = mock(AdminRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        injectField(service, "jwtUtil", jwtUtil);
        injectField(service, "request", request);
        injectField(service, "studentRepository", studentRepository);
        injectField(service, "teacherRepository", teacherRepository);
        injectField(service, "adminRepository", adminRepository);
        injectField(service, "passwordEncoder", passwordEncoder);
        injectField(service, "avatarDir", AVATAR_DIR);

        // 清理测试目录
        new File(AVATAR_DIR).delete();
    }

    @Test
    @DisplayName("saveAvatar: DB更新失败时旧头像文件不应被删除")
    void saveAvatar_DbUpdateFails_OldFileShouldNotBeDeleted() throws Exception {
        // 准备测试图片
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        byte[] imageBytes = createTestImageBytes(image);
        MultipartFile file = createMockMultipartFile(imageBytes, "image/png");

        // 设置Token
        when(request.getHeader("Authorization")).thenReturn("Bearer testToken");
        Claims claims = mock(Claims.class);
        when(jwtUtil.validateAccessToken("testToken")).thenReturn(claims);
        when(jwtUtil.getUserIdFromToken("testToken")).thenReturn(1L);
        when(jwtUtil.getRoleFromToken("testToken")).thenReturn("student");

        // 模拟已有旧头像
        Student existingStudent = new Student();
        existingStudent.setId(1L);
        existingStudent.setAvatarUrl(AVATAR_URL_PREFIX + "old_avatar.png");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existingStudent));

        // 创建旧头像文件
        File oldAvatarFile = new File(AVATAR_DIR + "old_avatar.png");
        oldAvatarFile.getParentFile().mkdirs();
        oldAvatarFile.createNewFile();

        // 模拟DB更新失败
        when(studentRepository.save(any())).thenThrow(new RuntimeException("DB连接失败"));

        // 执行saveAvatar，期望抛出异常
        assertThrows(RuntimeException.class, () -> service.saveAvatar(file));

        // 关键验证：旧头像文件应仍然存在（未被删除）
        assertTrue(oldAvatarFile.exists(), "DB更新失败时，旧头像文件不应被删除");
    }

    @Test
    @DisplayName("saveAvatar: 成功更新后旧头像文件应被删除")
    void saveAvatar_Success_OldFileShouldBeDeleted() throws Exception {
        // 准备测试图片
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        byte[] imageBytes = createTestImageBytes(image);
        MultipartFile file = createMockMultipartFile(imageBytes, "image/png");

        // 设置Token
        when(request.getHeader("Authorization")).thenReturn("Bearer testToken");
        Claims claims = mock(Claims.class);
        when(jwtUtil.validateAccessToken("testToken")).thenReturn(claims);
        when(jwtUtil.getUserIdFromToken("testToken")).thenReturn(1L);
        when(jwtUtil.getRoleFromToken("testToken")).thenReturn("student");

        // 模拟已有旧头像
        Student existingStudent = new Student();
        existingStudent.setId(1L);
        existingStudent.setAvatarUrl(AVATAR_URL_PREFIX + "old_avatar.png");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existingStudent));

        // 创建旧头像文件
        File oldAvatarFile = new File(AVATAR_DIR + "old_avatar.png");
        oldAvatarFile.getParentFile().mkdirs();
        oldAvatarFile.createNewFile();

        // 模拟DB更新成功
        when(studentRepository.save(any())).thenReturn(existingStudent);

        // 执行saveAvatar
        String result = service.saveAvatar(file);

        // 验证返回新头像URL
        assertNotNull(result);
        assertTrue(result.startsWith(AVATAR_URL_PREFIX));

        // 关键验证：旧头像文件应被删除
        assertFalse(oldAvatarFile.exists(), "成功更新后，旧头像文件应被删除");
    }

    @Test
    @DisplayName("saveAvatar: 新头像文件创建失败应不影响旧头像")
    void saveAvatar_NewFileCreationFails_OldFileShouldRemain() throws Exception {
        // 准备无效图片数据
        byte[] invalidBytes = new byte[] {0, 1, 2, 3};
        MultipartFile file = createMockMultipartFile(invalidBytes, "image/png");

        // 设置Token
        when(request.getHeader("Authorization")).thenReturn("Bearer testToken");
        Claims claims = mock(Claims.class);
        when(jwtUtil.validateAccessToken("testToken")).thenReturn(claims);
        when(jwtUtil.getUserIdFromToken("testToken")).thenReturn(1L);
        when(jwtUtil.getRoleFromToken("testToken")).thenReturn("student");

        // 模拟已有旧头像
        Student existingStudent = new Student();
        existingStudent.setId(1L);
        existingStudent.setAvatarUrl(AVATAR_URL_PREFIX + "old_avatar.png");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existingStudent));

        // 创建旧头像文件
        File oldAvatarFile = new File(AVATAR_DIR + "old_avatar.png");
        oldAvatarFile.getParentFile().mkdirs();
        oldAvatarFile.createNewFile();

        // 执行saveAvatar，期望抛出异常（无法解析图片）
        assertThrows(RuntimeException.class, () -> service.saveAvatar(file));

        // 验证：旧头像文件应仍然存在
        assertTrue(oldAvatarFile.exists(), "新文件创建失败时，旧头像文件不应被删除");
    }

    // ===== 工具方法 =====

    private byte[] createTestImageBytes(BufferedImage image) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private MultipartFile createMockMultipartFile(byte[] content, String contentType) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(content == null || content.length == 0);
        when(file.getContentType()).thenReturn(contentType);
        when(file.getSize()).thenReturn(content != null ? content.length : 0L);
        try {
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content != null ? content : new byte[0]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    private static void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            fail("无法注入 " + fieldName + ": " + e.getMessage());
        }
    }
}