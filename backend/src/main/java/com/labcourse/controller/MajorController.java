package com.labcourse.controller;

import com.labcourse.entity.Major;
import com.labcourse.service.MajorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/major")
@PreAuthorize("hasRole('admin')")
public class MajorController {

    @Autowired
    private MajorService majorService;

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", majorService.list(name, collegeId, status, page, size, sortBy, sortDir));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list/by-college/{collegeId}")
    public ResponseEntity<Map<String, Object>> listByCollege(@PathVariable Long collegeId) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", majorService.listByCollegeId(collegeId));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@Valid @RequestBody Major major) {
        Map<String, Object> result = new HashMap<>();
        boolean success = majorService.save(major);
        result.put("success", success);
        result.put("message", success ? "添加成功" : "添加失败，该学院下已存在同名专业");
        return ResponseEntity.ok(result);
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Major major) {
        Map<String, Object> result = new HashMap<>();
        boolean success = majorService.update(major);
        result.put("success", success);
        result.put("message", success ? "更新成功" : "更新失败");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = majorService.delete(id);
        return ResponseEntity.ok(result);
    }
}