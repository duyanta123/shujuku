package com.labcourse.service;

import com.labcourse.entity.Course;

import java.util.List;
import java.util.Map;

public interface CourseService {
    List<Map<String, Object>> getCourseList();
    List<Course> list(Long collegeId);
    boolean save(Course course);
    boolean updateById(Course course);
    boolean removeById(Long id);
}
