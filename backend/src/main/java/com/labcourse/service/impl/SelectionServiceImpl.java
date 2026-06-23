package com.labcourse.service.impl;

import com.labcourse.entity.Course;
import com.labcourse.entity.Selection;
import com.labcourse.entity.Student;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.SelectionRepository;
import com.labcourse.repository.StudentRepository;
import com.labcourse.service.SelectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class SelectionServiceImpl implements SelectionService {

    private static final Logger logger = LoggerFactory.getLogger(SelectionServiceImpl.class);

    @Autowired
    private SelectionRepository selectionRepository;
    
    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public boolean addSelection(Long studentId, Long courseId) {
        try {
            // Check course exists
            Optional<Course> courseOpt = courseRepository.findById(courseId);
            if (courseOpt.isEmpty()) {
                logger.warn("选课失败：课程 {} 不存在", courseId);
                return false;
            }
            Course course = courseOpt.get();

            // Check if course is REQUIRED - cannot manually select
            if ("REQUIRED".equals(course.getCourseType())) {
                logger.warn("选课失败：课程 {} 为必修课，由系统自动分配，无法手动选课", courseId);
                return false;
            }

            // Check college matching
            Optional<Student> studentOpt = studentRepository.findById(studentId);
            if (studentOpt.isPresent()) {
                Student student = studentOpt.get();
                if (student.getCollegeId() == null) {
                    logger.warn("选课失败：学生 {} 未设置学院", studentId);
                    return false;
                }
                if (course.getCollegeId() == null) {
                    logger.warn("选课失败：课程 {} 未设置所属学院", courseId);
                    return false;
                }
                if (!student.getCollegeId().equals(course.getCollegeId())) {
                    logger.warn("选课失败：学生 {} 学院ID={} 与课程 {} 学院ID={} 不匹配，仅可选择本学院的选修课",
                            studentId, student.getCollegeId(), courseId, course.getCollegeId());
                    return false;
                }
            } else {
                logger.warn("选课失败：学生 {} 不存在", studentId);
                return false;
            }

            // Call stored procedure for conflict check, capacity check and insert
            /*
             * 存储过程 proc_check_course_selection_conflict 参数类型映射：
             *   p_student_id   BIGINT(IN)   → Java: Long         → JDBC: Types.BIGINT
             *   p_course_id    BIGINT(IN)   → Java: Long         → JDBC: Types.BIGINT
             *   p_result_code  INT(OUT)     → Java: Integer      → JDBC: Types.INTEGER
             *   p_result_msg   VARCHAR(OUT) → Java: String       → JDBC: Types.VARCHAR
             */
            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("proc_check_course_selection_conflict")
                    .declareParameters(
                            new SqlParameter("p_student_id", Types.BIGINT),
                            new SqlParameter("p_course_id", Types.BIGINT),
                            new SqlOutParameter("p_result_code", Types.INTEGER),
                            new SqlOutParameter("p_result_msg", Types.VARCHAR)
                    );

            MapSqlParameterSource inParams = new MapSqlParameterSource()
                    .addValue("p_student_id", studentId)
                    .addValue("p_course_id", courseId);

            Map<String, Object> out = jdbcCall.execute(inParams);
            Integer resultCode = (Integer) out.get("p_result_code");
            String resultMsg = (String) out.get("p_result_msg");

            if (resultCode == null) {
                logger.error("选课异常：存储过程返回空结果码，学生 {} 课程 {}", studentId, courseId);
                return false;
            }

            switch (resultCode) {
                case 0:
                    logger.info("选课成功：学生 {} 选择了课程 {} ({})", studentId, courseId, course.getCourseName());
                    return true;
                case 1:
                    logger.warn("选课失败：学生 {} 已选择课程 {} - {}", studentId, courseId, resultMsg);
                    return false;
                case 2:
                    logger.warn("选课失败：课程 {} 容量已满 - {}", courseId, resultMsg);
                    return false;
                default:
                    logger.error("选课异常：存储过程返回未知结果码 {}，学生 {} 课程 {} - {}",
                            resultCode, studentId, courseId, resultMsg);
                    return false;
            }
        } catch (DataAccessException e) {
            logger.error("选课数据库异常：学生 {} 课程 {} - {}", studentId, courseId, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("选课异常：学生 {} 课程 {} - {}", studentId, courseId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteSelection(Long id, Long currentUserId) {
        // Security fix (MEDIUM): 验证选课记录所有权，防止IDOR攻击
        com.labcourse.entity.Selection selection = selectionRepository.findById(id).orElse(null);
        if (selection == null || !selection.getStudentId().equals(currentUserId)) {
            logger.warn("退课失败：选课记录 {} 不属于学生 {} 或不存在", id, currentUserId);
            return false;
        }
        selectionRepository.deleteById(id);
        logger.info("学生 {} 退课成功：选课记录 {}", currentUserId, id);
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
                sel.select_time
            FROM selection sel
            JOIN student s ON sel.student_id = s.id
            WHERE sel.course_id = ?
            ORDER BY s.student_no
            """;
        return jdbcTemplate.queryForList(sql, courseId);
    }
}
