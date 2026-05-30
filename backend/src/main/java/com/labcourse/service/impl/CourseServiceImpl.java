package com.labcourse.service.impl;

import com.labcourse.entity.Course;
import com.labcourse.repository.CourseRepository;
import com.labcourse.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> getCourseList() {
        String sql = """
            SELECT
                c.id,
                c.course_name,
                t.name AS teacher_name,
                l.lab_name,
                l.location,
                c.course_time,
                c.max_count,
                COUNT(s.id) AS selected_count
            FROM course c
            LEFT JOIN teacher t ON c.teacher_id = t.id
            LEFT JOIN lab l ON c.lab_id = l.id
            LEFT JOIN selection s ON c.id = s.course_id
            GROUP BY c.id, c.course_name, t.name, l.lab_name, l.location, c.course_time, c.max_count
            ORDER BY c.id
            """;
        return jdbcTemplate.queryForList(sql);
    }

    @Override
    public List<Course> list() {
        return courseRepository.findAll();
    }

    @Override
    public boolean save(Course course) {
        courseRepository.save(course);
        return true;
    }

    @Override
    public boolean updateById(Course course) {
        courseRepository.save(course);
        return true;
    }

    @Override
    public boolean removeById(Long id) {
        courseRepository.deleteById(id);
        return true;
    }
}
