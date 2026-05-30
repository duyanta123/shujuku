package com.labcourse.controller;

import com.labcourse.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@RequestBody Map<String, Object> data) {
        Long studentId = Long.valueOf(data.get("studentId").toString());
        Long courseId = Long.valueOf(data.get("courseId").toString());
        String status = data.get("status").toString();

        Map<String, Object> result = new HashMap<>();
        boolean success = attendanceService.addAttendance(studentId, courseId, status);

        result.put("success", success);
        result.put("message", success ? "考勤录入成功" : "考勤录入失败");
        return ResponseEntity.ok(result);
    }
}
