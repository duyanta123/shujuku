package com.labcourse.controller;

import com.labcourse.entity.College;
import com.labcourse.service.CollegeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/college")
@PreAuthorize("hasRole('admin')")
public class CollegeController {

    @Autowired
    private CollegeService collegeService;

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", collegeService.list(name, status, page, size, sortBy, sortDir));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@Valid @RequestBody College college) {
        Map<String, Object> result = new HashMap<>();
        boolean success = collegeService.save(college);
        result.put("success", success);
        result.put("message", success ? "添加成功" : "添加失败，学院名称可能重复");
        return ResponseEntity.ok(result);
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody College college) {
        Map<String, Object> result = new HashMap<>();
        boolean success = collegeService.update(college);
        result.put("success", success);
        result.put("message", success ? "更新成功" : "更新失败");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = collegeService.delete(id);
        return ResponseEntity.ok(result);
    }
}