package com.labcourse.service.impl;

import com.labcourse.entity.Course;
import com.labcourse.entity.Major;
import com.labcourse.entity.MajorRequiredCourse;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.MajorRepository;
import com.labcourse.repository.MajorRequiredCourseRepository;
import com.labcourse.service.MajorRequiredCourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class MajorRequiredCourseServiceImpl implements MajorRequiredCourseService {

    private static final Logger logger = LoggerFactory.getLogger(MajorRequiredCourseServiceImpl.class);

    @Autowired
    private MajorRequiredCourseRepository majorRequiredCourseRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> bind(Long majorId, Long courseId) {
        Map<String, Object> result = new HashMap<>();

        // Validate course exists
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            result.put("success", false);
            result.put("message", "课程不存在");
            return result;
        }

        // Validate course type is REQUIRED
        if (!"REQUIRED".equals(course.getCourseType())) {
            result.put("success", false);
            result.put("message", "仅可绑定必修课类型的课程");
            return result;
        }

        // Validate course college_id matches major's college_id
        Major major = majorRepository.findById(majorId).orElse(null);
        if (major == null) {
            result.put("success", false);
            result.put("message", "专业不存在");
            return result;
        }

        if (course.getCollegeId() == null || !course.getCollegeId().equals(major.getCollegeId())) {
            result.put("success", false);
            result.put("message", "不可跨学院绑定必修课，课程和专业必须属于同一学院");
            return result;
        }

        // Check duplicate
        if (majorRequiredCourseRepository.findByMajorIdAndCourseId(majorId, courseId).isPresent()) {
            result.put("success", false);
            result.put("message", "该专业已绑定此必修课");
            return result;
        }

        MajorRequiredCourse mrc = new MajorRequiredCourse();
        mrc.setMajorId(majorId);
        mrc.setCourseId(courseId);
        majorRequiredCourseRepository.save(mrc);
        logger.info("管理员操作 - 绑定必修课: 专业ID={}, 课程ID={} ({})", majorId, courseId, course.getCourseName());
        result.put("success", true);
        result.put("message", "绑定成功");
        return result;
    }

    @Override
    public boolean unbind(Long majorId, Long courseId) {
        majorRequiredCourseRepository.deleteByMajorIdAndCourseId(majorId, courseId);
        logger.info("管理员操作 - 解除绑定必修课: 专业ID={}, 课程ID={}", majorId, courseId);
        return true;
    }

    @Override
    public List<Map<String, Object>> listByMajor(Long majorId) {
        String sql = """
            SELECT
                mrc.id,
                mrc.major_id,
                mrc.course_id,
                c.course_name,
                c.college,
                c.course_time,
                c.max_count,
                t.name AS teacher_name
            FROM major_required_course mrc
            JOIN course c ON mrc.course_id = c.id
            LEFT JOIN teacher t ON c.teacher_id = t.id
            WHERE mrc.major_id = ?
            ORDER BY mrc.id
            """;
        return jdbcTemplate.queryForList(sql, majorId);
    }
}