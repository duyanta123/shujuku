package com.labcourse.controller;

import com.labcourse.service.SelectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/selection")
public class SelectionController {

    @Autowired
    private SelectionService selectionService;

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@RequestBody Map<String, Long> data) {
        Long studentId = data.get("studentId");
        Long courseId = data.get("courseId");

        Map<String, Object> result = new HashMap<>();
        boolean success = selectionService.addSelection(studentId, courseId);

        result.put("success", success);
        result.put("message", success ? "选课成功" : "选课失败（已选或人数已满）");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        boolean success = selectionService.deleteSelection(id);
        result.put("success", success);
        result.put("message", success ? "退课成功" : "退课失败");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my/{studentId}")
    public ResponseEntity<Map<String, Object>> myCourses(@PathVariable Long studentId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> courses = selectionService.getMyCourses(studentId);
        result.put("success", true);
        result.put("data", courses);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/studentList/{courseId}")
    public ResponseEntity<Map<String, Object>> studentList(@PathVariable Long courseId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> students = selectionService.getStudentList(courseId);
        result.put("success", true);
        result.put("data", students);
        return ResponseEntity.ok(result);
    }
}
