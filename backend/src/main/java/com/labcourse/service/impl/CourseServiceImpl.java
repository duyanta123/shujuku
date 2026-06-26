package com.labcourse.service.impl;

import com.labcourse.entity.Course;
import com.labcourse.entity.College;
import com.labcourse.entity.Lab;
import com.labcourse.entity.Teacher;
import com.labcourse.exception.BusinessException;
import com.labcourse.repository.CollegeRepository;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.LabRepository;
import com.labcourse.repository.SelectionRepository;
import com.labcourse.repository.TeacherRepository;
import com.labcourse.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class CourseServiceImpl implements CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseServiceImpl.class);

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SelectionRepository selectionRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private LabRepository labRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Map<String, Object>> getCourseList() {
        String sql = """
            SELECT
                c.id,
                c.course_name,
                c.course_name AS courseName,
                t.name AS teacher_name,
                t.name AS teacherName,
                l.lab_name,
                l.lab_name AS labName,
                l.location,
                c.course_time,
                c.course_time AS courseTime,
                c.max_count,
                c.max_count AS maxCount,
                c.college_id,
                c.college_id AS collegeId,
                col.name AS college,
                c.course_type,
                c.course_type AS courseType,
                COUNT(s.id) AS selected_count,
                COUNT(s.id) AS selectedCount
            FROM course c
            LEFT JOIN teacher t ON c.teacher_id = t.id
            LEFT JOIN lab l ON c.lab_id = l.id
            LEFT JOIN college col ON c.college_id = col.id
            LEFT JOIN selection s ON c.id = s.course_id
            GROUP BY c.id, c.course_name, t.name, l.lab_name, l.location, c.course_time, c.max_count, c.college_id, col.name, c.course_type
            ORDER BY c.id
            """;
        return jdbcTemplate.queryForList(sql);
    }

    @Override
    public List<Map<String, Object>> getCourseListByTeacherId(Long teacherId) {
        String sql = """
            SELECT
                c.id,
                c.course_name,
                c.course_name AS courseName,
                t.name AS teacher_name,
                t.name AS teacherName,
                l.lab_name,
                l.lab_name AS labName,
                l.location,
                c.course_time,
                c.course_time AS courseTime,
                c.max_count,
                c.max_count AS maxCount,
                c.college_id,
                c.college_id AS collegeId,
                col.name AS college,
                c.course_type,
                c.course_type AS courseType,
                COUNT(s.id) AS selected_count,
                COUNT(s.id) AS selectedCount
            FROM course c
            LEFT JOIN teacher t ON c.teacher_id = t.id
            LEFT JOIN lab l ON c.lab_id = l.id
            LEFT JOIN college col ON c.college_id = col.id
            LEFT JOIN selection s ON c.id = s.course_id
            WHERE c.teacher_id = ?
            GROUP BY c.id, c.course_name, t.name, l.lab_name, l.location, c.course_time, c.max_count, c.college_id, col.name, c.course_type
            ORDER BY c.id
            """;
        return jdbcTemplate.queryForList(sql, teacherId);
    }

    @Override
    public List<Course> list(Long collegeId) {
        if (collegeId == null) {
            return courseRepository.findAll();
        }
        return courseRepository.findByCollegeId(collegeId);
    }

    @Override
    public List<Map<String, Object>> listSimple(Long collegeId) {
        String sql = """
            SELECT
                c.id,
                c.course_name,
                c.course_name AS courseName,
                c.teacher_id,
                c.teacher_id AS teacherId,
                t.name AS teacher_name,
                t.name AS teacherName,
                c.lab_id,
                c.lab_id AS labId,
                l.lab_name,
                l.lab_name AS labName,
                l.location,
                c.course_time,
                c.course_time AS courseTime,
                c.max_count,
                c.max_count AS maxCount,
                c.college_id,
                c.college_id AS collegeId,
                col.name AS college,
                c.course_type,
                c.course_type AS courseType,
                COUNT(s.id) AS selected_count,
                COUNT(s.id) AS selectedCount
            FROM course c
            LEFT JOIN teacher t ON c.teacher_id = t.id
            LEFT JOIN lab l ON c.lab_id = l.id
            LEFT JOIN college col ON c.college_id = col.id
            LEFT JOIN selection s ON c.id = s.course_id
            WHERE (? IS NULL OR c.college_id = ?)
            GROUP BY c.id, c.course_name, c.teacher_id, t.name, c.lab_id, l.lab_name, l.location,
                     c.course_time, c.max_count, c.college_id, col.name, c.course_type
            ORDER BY c.id
            """;
        return jdbcTemplate.queryForList(sql, collegeId, collegeId);
    }

    @Override
    public boolean save(Course course) {
        validateCourse(course);
        courseRepository.save(course);
        return true;
    }

    @Override
    public boolean updateById(Course course) {
        Optional<Course> existingOpt = courseRepository.findById(course.getId());
        if (existingOpt.isPresent()) {
            Course existing = existingOpt.get();
            if (course.getMaxCount() != null) {
                validateMaxCount(course.getMaxCount());
            }
            if (course.getCourseTime() != null) {
                validateCourseTime(course.getCourseTime());
            }
            Course candidate = new Course();
            candidate.setId(existing.getId());
            candidate.setCourseName(course.getCourseName() != null ? course.getCourseName() : existing.getCourseName());
            candidate.setTeacherId(course.getTeacherId() != null ? course.getTeacherId() : existing.getTeacherId());
            candidate.setLabId(course.getLabId() != null ? course.getLabId() : existing.getLabId());
            candidate.setCourseTime(course.getCourseTime() != null ? course.getCourseTime() : existing.getCourseTime());
            candidate.setMaxCount(course.getMaxCount() != null ? course.getMaxCount() : existing.getMaxCount());
            candidate.setCollegeId(course.getCollegeId() != null ? course.getCollegeId() : existing.getCollegeId());
            candidate.setCourseType(course.getCourseType() != null ? course.getCourseType() : existing.getCourseType());
            if (course.getCourseType() != null) {
                validateCourseType(course.getCourseType());
            }
            boolean relationChanged =
                    (course.getTeacherId() != null && !course.getTeacherId().equals(existing.getTeacherId()))
                    || (course.getLabId() != null && !course.getLabId().equals(existing.getLabId()))
                    || (course.getCollegeId() != null && !course.getCollegeId().equals(existing.getCollegeId()));
            if (relationChanged) {
                validateCourseRelations(candidate);
            }
            if (course.getCourseName() != null) { existing.setCourseName(course.getCourseName()); }
            if (course.getTeacherId() != null) { existing.setTeacherId(course.getTeacherId()); }
            if (course.getLabId() != null) { existing.setLabId(course.getLabId()); }
            if (course.getCourseTime() != null) { existing.setCourseTime(course.getCourseTime()); }
            if (course.getMaxCount() != null) { existing.setMaxCount(course.getMaxCount()); }
            if (course.getCollegeId() != null) { existing.setCollegeId(course.getCollegeId()); }
            if (course.getCourseType() != null) {
                if (existing.getCourseType() != null && !existing.getCourseType().equals(course.getCourseType())) {
                    if (selectionRepository.existsByCourseId(course.getId())) {
                        logger.warn("修改课程类型失败：课程 {} 已有学生选课，无法修改课程类型", course.getId());
                        throw new BusinessException("COURSE_HAS_SELECTIONS", "已有学生选课，无法修改课程类型", HttpStatus.CONFLICT);
                    }
                }
                existing.setCourseType(course.getCourseType());
            }
            courseRepository.save(existing);
            return true;
        }
        return false;
    }

    private void validateCourse(Course course) {
        validateMaxCount(course.getMaxCount());
        validateCourseTime(course.getCourseTime());
        validateCourseType(course.getCourseType());
        validateCourseRelations(course);
    }

    private void validateMaxCount(Integer maxCount) {
        if (maxCount == null || maxCount < 1 || maxCount > 100) {
            throw new IllegalArgumentException("课程容量范围为1-100");
        }
    }

    private void validateCourseTime(String courseTime) {
        if (!hasValidCourseTime(courseTime)) {
            throw new IllegalArgumentException("课程时间格式无效");
        }
    }

    private boolean hasValidCourseTime(String courseTime) {
        if (courseTime == null || courseTime.isBlank()) {
            return false;
        }
        String[] parts = courseTime.split("[,，]");
        java.util.regex.Pattern dayPattern = java.util.regex.Pattern.compile("(周[一二三四五六日]|星期[一二三四五六日])");
        java.util.regex.Pattern periodPattern = java.util.regex.Pattern.compile("(\\d+)-(\\d+)节");
        for (String part : parts) {
            java.util.regex.Matcher dayMatcher = dayPattern.matcher(part);
            java.util.regex.Matcher periodMatcher = periodPattern.matcher(part);
            if (!dayMatcher.find() || !periodMatcher.find()) {
                continue;
            }
            int start = Integer.parseInt(periodMatcher.group(1));
            int end = Integer.parseInt(periodMatcher.group(2));
            if (start >= 1 && end <= 10 && start <= end) {
                return true;
            }
        }
        return false;
    }

    private void validateCourseType(String courseType) {
        if (courseType == null || (!"REQUIRED".equals(courseType) && !"ELECTIVE".equals(courseType))) {
            throw new BusinessException("INVALID_COURSE_TYPE", "courseType must be REQUIRED or ELECTIVE", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateCourseRelations(Course course) {
        if (course.getTeacherId() == null) {
            throw new BusinessException("TEACHER_NOT_FOUND", "教师不存在", HttpStatus.BAD_REQUEST);
        }
        if (course.getCollegeId() == null) {
            throw new BusinessException("COLLEGE_NOT_FOUND", "学院不存在", HttpStatus.BAD_REQUEST);
        }
        Teacher teacher = teacherRepository.findById(course.getTeacherId())
                .orElseThrow(() -> new BusinessException("TEACHER_NOT_FOUND", "教师不存在", HttpStatus.BAD_REQUEST));
        College college = collegeRepository.findById(course.getCollegeId())
                .orElseThrow(() -> new BusinessException("COLLEGE_NOT_FOUND", "学院不存在", HttpStatus.BAD_REQUEST));
        if (!"ACTIVE".equals(college.getStatus())) {
            throw new BusinessException("INVALID_RELATION", "学院已停用", HttpStatus.BAD_REQUEST);
        }
        if (teacher.getCollegeId() != null && !teacher.getCollegeId().equals(course.getCollegeId())) {
            throw new BusinessException("INVALID_RELATION", "教师与课程必须属于同一学院", HttpStatus.BAD_REQUEST);
        }
        if (course.getLabId() != null) {
            Lab lab = labRepository.findById(course.getLabId())
                    .orElseThrow(() -> new BusinessException("LAB_NOT_FOUND", "实验室不存在", HttpStatus.BAD_REQUEST));
            if (lab.getCollegeId() != null && !lab.getCollegeId().equals(course.getCollegeId())) {
                throw new BusinessException("INVALID_RELATION", "实验室与课程必须属于同一学院", HttpStatus.BAD_REQUEST);
            }
        }
    }

    @Override
    @Transactional
    public boolean removeById(Long id) {
        if (!courseRepository.existsById(id)) {
            logger.warn("删除课程失败：课程 {} 不存在", id);
            return false;
        }
        if (selectionRepository.existsByCourseId(id)) {
            throw new BusinessException("COURSE_HAS_SELECTIONS", "课程已有选课记录，无法删除", HttpStatus.CONFLICT);
        }

        // 注意：以下关联表需要手动清理（JPA 未配置级联删除）：
        // - selection (选课表)
        // - score (成绩表)
        // - attendance (考勤表)
        int deletedSelections = jdbcTemplate.update("DELETE FROM selection WHERE course_id = ?", id);
        int deletedScores = jdbcTemplate.update("DELETE FROM score WHERE course_id = ?", id);
        int deletedAttendances = jdbcTemplate.update("DELETE FROM attendance WHERE course_id = ?", id);
        int deletedMajorRequiredCourses = jdbcTemplate.update("DELETE FROM major_required_course WHERE course_id = ?", id);
        logger.info("删除课程 {} 的关联数据：选课 {} 条，成绩 {} 条，考勤 {} 条，专业必修关联 {} 条",
                id, deletedSelections, deletedScores, deletedAttendances, deletedMajorRequiredCourses);

        courseRepository.deleteById(id);
        logger.info("课程 {} 删除成功", id);
        return true;
    }
}
