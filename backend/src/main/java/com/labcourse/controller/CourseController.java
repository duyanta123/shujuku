package com.labcourse.controller;

import com.labcourse.entity.Course;
import com.labcourse.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/course")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> courses = courseService.getCourseList();
        result.put("success", true);
        result.put("data", courses);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list/simple")
    public ResponseEntity<Map<String, Object>> listSimple() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", courseService.list());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@RequestBody Course course) {
        Map<String, Object> result = new HashMap<>();
        boolean success = courseService.save(course);
        result.put("success", success);
        result.put("message", success ? "添加成功" : "添加失败");
        return ResponseEntity.ok(result);
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> update(@RequestBody Course course) {
        Map<String, Object> result = new HashMap<>();
        boolean success = courseService.updateById(course);
        result.put("success", success);
        result.put("message", success ? "更新成功" : "更新失败");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        boolean success = courseService.removeById(id);
        result.put("success", success);
        result.put("message", success ? "删除成功" : "删除失败");
        return ResponseEntity.ok(result);
    }
}
