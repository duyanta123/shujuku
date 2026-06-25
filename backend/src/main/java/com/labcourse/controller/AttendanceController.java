package com.labcourse.controller;

import com.labcourse.entity.Course;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.SelectionRepository;
import com.labcourse.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SelectionRepository selectionRepository;

    /**
     * 学生签到 - 自动判定出勤/迟到
     */
    @PostMapping("/check-in")
    public ResponseEntity<Map<String, Object>> checkIn(@RequestBody Map<String, Object> data) {
        Object courseIdObj = data.get("courseId");

        if (courseIdObj == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "courseId 不能为空");
            return ResponseEntity.badRequest().body(result);
        }

        Long courseId;
        try {
            courseId = Long.valueOf(courseIdObj.toString());
        } catch (NumberFormatException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Invalid id parameter");
            return ResponseEntity.badRequest().body(result);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!hasRole(authentication, "student")) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "仅学生可签到");
            return ResponseEntity.status(403).body(result);
        }
        Long studentId = Long.valueOf(authentication.getPrincipal().toString());

        if (courseRepository.existsById(courseId)
                && selectionRepository.findByStudentIdAndCourseId(studentId, courseId).isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "未选修该课程，无法签到");
            return ResponseEntity.status(403).body(result);
        }

        Map<String, Object> result = attendanceService.checkIn(studentId, courseId);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取学生考勤历史
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(@RequestParam(required = false) Long studentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> result = new HashMap<>();

        if (hasRole(authentication, "student")) {
            studentId = Long.valueOf(authentication.getPrincipal().toString());
        } else if (hasRole(authentication, "teacher")) {
            if (studentId == null) {
                result.put("success", false);
                result.put("message", "studentId is required");
                return ResponseEntity.badRequest().body(result);
            }
            Long teacherId = Long.valueOf(authentication.getPrincipal().toString());
            if (!isStudentInTeacherCourses(teacherId, studentId)) {
                result.put("success", false);
                result.put("message", "无权查看该学生的考勤记录");
                return ResponseEntity.status(403).body(result);
            }
        } else if (studentId == null) {
            result.put("success", false);
            result.put("message", "studentId is required");
            return ResponseEntity.badRequest().body(result);
        }

        List<Map<String, Object>> records = attendanceService.getStudentHistory(studentId);
        result.put("success", true);
        result.put("data", records);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取某课程某天的考勤列表（教师端）
     */
    @GetMapping("/course")
    public ResponseEntity<Map<String, Object>> getCourseAttendance(
            @RequestParam Long courseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ResponseEntity<Map<String, Object>> ownershipCheck = validateTeacherCourseOwnership(courseId);
        if (ownershipCheck != null) return ownershipCheck;
        List<Map<String, Object>> records = attendanceService.getCourseAttendance(courseId, date);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", records);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取课程的考勤日期列表
     */
    @GetMapping("/dates")
    public ResponseEntity<Map<String, Object>> getAttendanceDates(@RequestParam Long courseId) {
        ResponseEntity<Map<String, Object>> ownershipCheck = validateTeacherCourseOwnership(courseId);
        if (ownershipCheck != null) return ownershipCheck;
        List<LocalDate> dates = attendanceService.getAttendanceDates(courseId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", dates);
        return ResponseEntity.ok(result);
    }

    /**
     * 教师修改考勤状态（仅 缺勤→请假）
     */
    @PutMapping("/update-status")
    public ResponseEntity<Map<String, Object>> updateStatus(@RequestBody Map<String, Object> data) {
        Object attendanceIdObj = data.get("attendanceId");
        Object newStatusObj = data.get("newStatus");
        if (attendanceIdObj == null || newStatusObj == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "attendanceId and newStatus cannot be empty");
            return ResponseEntity.badRequest().body(result);
        }
        Long attendanceId;
        try {
            attendanceId = Long.valueOf(attendanceIdObj.toString());
        } catch (NumberFormatException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Invalid attendanceId");
            return ResponseEntity.badRequest().body(result);
        }
        String newStatus = newStatusObj.toString();

        // Security fix: teacherId从JWT Token中获取，不信任请求体
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!hasRole(authentication, "teacher")) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "仅教师可修改考勤");
            return ResponseEntity.status(403).body(result);
        }
        Long teacherId = Long.valueOf(authentication.getPrincipal().toString());

        String reason = data.get("reason") != null ? data.get("reason").toString() : "";

        Map<String, Object> result = attendanceService.updateAttendanceStatus(attendanceId, newStatus, teacherId, reason);
        return ResponseEntity.ok(result);
    }

    /**
     * 导出考勤数据
     */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> export(@RequestParam Long courseId) {
        ResponseEntity<Map<String, Object>> ownershipCheck = validateTeacherCourseOwnership(courseId);
        if (ownershipCheck != null) return ownershipCheck;
        List<Map<String, Object>> records = attendanceService.exportAttendance(courseId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", records);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取服务器时间
     */
    @GetMapping("/server-time")
    public ResponseEntity<Map<String, Object>> serverTime() {
        Map<String, Object> result = attendanceService.getServerTime();
        return ResponseEntity.ok(result);
    }

    /**
     * 批量创建缺勤记录（对未签到的学生）
     */
    @PostMapping("/batch-absent")
    public ResponseEntity<Map<String, Object>> batchAbsent(@RequestBody Map<String, Object> data) {
        Object courseIdObj = data.get("courseId");
        if (courseIdObj == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "courseId cannot be empty");
            return ResponseEntity.badRequest().body(result);
        }
        Long courseId;
        try {
            courseId = Long.valueOf(courseIdObj.toString());
        } catch (NumberFormatException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Invalid courseId");
            return ResponseEntity.badRequest().body(result);
        }
        ResponseEntity<Map<String, Object>> ownershipCheck = validateTeacherCourseOwnership(courseId);
        if (ownershipCheck != null) return ownershipCheck;
        // 此功能简化：返回提示，实际缺勤由前端查询时自动展示
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "缺勤记录由系统自动判定");
        return ResponseEntity.ok(result);
    }

    /**
     * 验证教师是否有权操作指定课程
     */
    private ResponseEntity<Map<String, Object>> validateTeacherCourseOwnership(Long courseId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!hasRole(authentication, "teacher")) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "仅教师可操作课程考勤");
            return ResponseEntity.status(403).body(result);
        }
        Long teacherId = Long.valueOf(authentication.getPrincipal().toString());
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "课程不存在");
            return ResponseEntity.status(403).body(result);
        }
        if (!course.getTeacherId().equals(teacherId)) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无权查看此课程的考勤记录");
            return ResponseEntity.status(403).body(result);
        }
        return null;
    }

    // ===== 兼容旧接口 =====

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@RequestBody Map<String, Object> data) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!hasRole(authentication, "teacher")) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "仅教师可录入考勤");
            return ResponseEntity.status(403).body(result);
        }

        Object studentIdObj = data.get("studentId");
        Object courseIdObj = data.get("courseId");
        Object statusObj = data.get("status");
        if (studentIdObj == null || courseIdObj == null || statusObj == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "studentId, courseId, status cannot be empty");
            return ResponseEntity.badRequest().body(result);
        }

        Long studentId;
        Long courseId;
        try {
            studentId = Long.valueOf(studentIdObj.toString());
            courseId = Long.valueOf(courseIdObj.toString());
        } catch (NumberFormatException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Invalid id parameter");
            return ResponseEntity.badRequest().body(result);
        }
        String status = statusObj.toString();

        ResponseEntity<Map<String, Object>> ownershipCheck = validateTeacherCourseOwnership(courseId);
        if (ownershipCheck != null) return ownershipCheck;
        if (selectionRepository.findByStudentIdAndCourseId(studentId, courseId).isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "学生未选修该课程");
            return ResponseEntity.status(403).body(result);
        }
        try {
            com.labcourse.entity.AttendanceStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无效的考勤状态");
            return ResponseEntity.badRequest().body(result);
        }

        Map<String, Object> result = new HashMap<>();
        boolean success = attendanceService.addAttendance(studentId, courseId, status);

        result.put("success", success);
        result.put("message", success ? "考勤录入成功" : "考勤录入失败");
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

    private boolean isStudentInTeacherCourses(Long teacherId, Long studentId) {
        List<Course> teacherCourses = courseRepository.findByTeacherId(teacherId);
        for (Course course : teacherCourses) {
            if (selectionRepository.findByStudentIdAndCourseId(studentId, course.getId()).isPresent()) {
                return true;
            }
        }
        return false;
    }
}
