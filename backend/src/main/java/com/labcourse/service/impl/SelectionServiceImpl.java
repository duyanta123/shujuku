package com.labcourse.service.impl;

import com.labcourse.entity.Course;
import com.labcourse.entity.Selection;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.SelectionRepository;
import com.labcourse.service.SelectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class SelectionServiceImpl implements SelectionService {

    @Autowired
    private SelectionRepository selectionRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public boolean addSelection(Long studentId, Long courseId) {
        try {
            if (selectionRepository.findByStudentIdAndCourseId(studentId, courseId).isPresent()) {
                return false;
            }

            Long count = selectionRepository.countByCourseId(courseId);
            Course course = courseRepository.findById(courseId).orElse(null);
            
            if (course != null && count >= course.getMaxCount()) {
                return false;
            }

            Selection selection = new Selection();
            selection.setStudentId(studentId);
            selection.setCourseId(courseId);
            selectionRepository.save(selection);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteSelection(Long id) {
        selectionRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Map<String, Object>> getMyCourses(Long studentId) {
        String sql = """
            SELECT
                sel.id AS selection_id,
                c.id AS course_id,
                c.course_name,
                t.name AS teacher_name,
                l.lab_name,
                l.location,
                c.course_time,
                sel.select_time
            FROM selection sel
            JOIN course c ON sel.course_id = c.id
            JOIN teacher t ON c.teacher_id = t.id
            LEFT JOIN lab l ON c.lab_id = l.id
            WHERE sel.student_id = ?
            ORDER BY sel.select_time DESC
            """;
        return jdbcTemplate.queryForList(sql, studentId);
    }

    @Override
    public List<Map<String, Object>> getStudentList(Long courseId) {
        String sql = """
            SELECT
                s.id AS student_id,
                s.student_no,
                s.name,
                s.gender,
                s.major,
                sel.select_time
            FROM selection sel
            JOIN student s ON sel.student_id = s.id
            WHERE sel.course_id = ?
            ORDER BY s.student_no
            """;
        return jdbcTemplate.queryForList(sql, courseId);
    }
}
