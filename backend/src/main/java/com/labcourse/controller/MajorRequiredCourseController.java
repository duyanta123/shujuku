package com.labcourse.controller;

import com.labcourse.service.MajorRequiredCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/major-required-course")
@PreAuthorize("hasRole('admin')")
public class MajorRequiredCourseController {

    @Autowired
    private MajorRequiredCourseService majorRequiredCourseService;

    @GetMapping("/list/by-major/{majorId}")
    public ResponseEntity<Map<String, Object>> listByMajor(@PathVariable Long majorId) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", majorRequiredCourseService.listByMajor(majorId));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bind")
    public ResponseEntity<Map<String, Object>> bind(@RequestBody Map<String, Object> data) {
        Long majorId = toLong(data.get("majorId"));
        Long courseId = toLong(data.get("courseId"));
        Map<String, Object> result = new HashMap<>();
        if (majorId == null || courseId == null || majorId <= 0 || courseId <= 0) {
            result.put("success", false);
            result.put("code", "INVALID_PARAMETER");
            result.put("message", "majorId and courseId are required");
            return ResponseEntity.badRequest().body(result);
        }
        result = majorRequiredCourseService.bind(majorId, courseId);
        return Boolean.TRUE.equals(result.get("success"))
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    }

    @DeleteMapping("/unbind/{majorId}/{courseId}")
    public ResponseEntity<Map<String, Object>> unbind(@PathVariable Long majorId, @PathVariable Long courseId) {
        boolean success = majorRequiredCourseService.unbind(majorId, courseId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "解绑成功" : "解绑失败");
        return ResponseEntity.ok(result);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
