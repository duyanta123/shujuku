package com.labcourse.controller;

import com.labcourse.entity.Lab;
import com.labcourse.service.LabService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/lab")
public class LabController {

    @Autowired
    private LabService labService;

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", labService.list());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@Valid @RequestBody Lab lab) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = labService.save(lab);
            result.put("success", success);
            result.put("message", success ? "添加成功" : "添加失败");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> update(@Valid @RequestBody Lab lab) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = labService.updateById(lab);
            result.put("success", success);
            result.put("message", success ? "更新成功" : "更新失败");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        boolean success = labService.removeById(id);
        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败");
        return ResponseEntity.ok(result);
    }
}
