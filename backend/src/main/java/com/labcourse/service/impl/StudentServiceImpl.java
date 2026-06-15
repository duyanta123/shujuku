package com.labcourse.service.impl;

import com.labcourse.entity.Course;
import com.labcourse.entity.MajorRequiredCourse;
import com.labcourse.entity.Selection;
import com.labcourse.entity.Student;
import com.labcourse.exception.AccountLockedException;
import com.labcourse.repository.CourseRepository;
import com.labcourse.repository.MajorRequiredCourseRepository;
import com.labcourse.repository.SelectionRepository;
import com.labcourse.repository.StudentRepository;
import com.labcourse.service.LoginAttemptService;
import com.labcourse.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class StudentServiceImpl implements StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private MajorRequiredCourseRepository majorRequiredCourseRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SelectionRepository selectionRepository;

    @Override
    public Student login(String studentNo, String password) {
        String key = "student:" + studentNo;

        LoginAttemptService.LoginResult checkResult = loginAttemptService.checkLoginAttempt(key);
        if (!checkResult.isAllowed()) {
            throw new AccountLockedException(
                    "账号已被锁定，请" + checkResult.getRemainingLockMinutes() + "分钟后再试",
                    checkResult.getRemainingLockMinutes());
        }

        Optional<Student> studentOpt = studentRepository.findByStudentNo(studentNo);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            if (passwordEncoder.matches(password, student.getPassword())) {
                loginAttemptService.resetAttempts(key);
                return student;
            }
        }

        loginAttemptService.recordFailedAttempt(key);
        return null;
    }

    @Override
    public List<Student> list() {
        return studentRepository.findAll();
    }

    @Override
    public boolean save(Student student) {
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        studentRepository.save(student);
        if (student.getMajorId() != null) {
            assignRequiredCourses(student.getId(), student.getMajorId());
        }
        return true;
    }

    @Override
    public boolean updateById(Student student) {
        Optional<Student> existingOpt = studentRepository.findById(student.getId());
        if (existingOpt.isPresent()) {
            Student existing = existingOpt.get();
            Long oldMajorId = existing.getMajorId();
            if (student.getStudentNo() != null && !student.getStudentNo().isEmpty()) {
                existing.setStudentNo(student.getStudentNo());
            }
            if (student.getName() != null && !student.getName().isEmpty()) {
                existing.setName(student.getName());
            }
            if (student.getGender() != null) {
                existing.setGender(student.getGender());
            }
            if (student.getMajor() != null) {
                existing.setMajor(student.getMajor());
            }
            if (student.getCollege() != null) {
                existing.setCollege(student.getCollege());
            }
            if (student.getCollegeId() != null) {
                existing.setCollegeId(student.getCollegeId());
            }
            if (student.getMajorId() != null) {
                existing.setMajorId(student.getMajorId());
            }
            if (student.getPassword() != null && !student.getPassword().isEmpty()) {
                existing.setPassword(passwordEncoder.encode(student.getPassword()));
            }
            studentRepository.save(existing);

            Long newMajorId = student.getMajorId() != null ? student.getMajorId() : oldMajorId;
            if (oldMajorId != null && student.getMajorId() != null && !oldMajorId.equals(student.getMajorId())) {
                // Major changed: remove old required courses, assign new ones
                List<MajorRequiredCourse> oldRequiredCourses = majorRequiredCourseRepository.findByMajorId(oldMajorId);
                for (MajorRequiredCourse mrc : oldRequiredCourses) {
                    Optional<Selection> existingSelection = selectionRepository.findByStudentIdAndCourseId(student.getId(), mrc.getCourseId());
                    if (existingSelection.isPresent()) {
                        selectionRepository.delete(existingSelection.get());
                        logger.info("学生 {} 专业变更，移除旧必修课选课记录: 课程ID={}", student.getId(), mrc.getCourseId());
                    }
                }
                assignRequiredCourses(student.getId(), newMajorId);
                logger.info("学生 {} 专业变更: 旧专业ID={}, 新专业ID={}", student.getId(), oldMajorId, newMajorId);
            } else if (oldMajorId == null && student.getMajorId() != null) {
                assignRequiredCourses(student.getId(), student.getMajorId());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean removeById(Long id) {
        studentRepository.deleteById(id);
        return true;
    }

    private void assignRequiredCourses(Long studentId, Long majorId) {
        List<MajorRequiredCourse> requiredCourses = majorRequiredCourseRepository.findByMajorId(majorId);
        if (requiredCourses.isEmpty()) {
            logger.info("专业 {} 无必修课配置", majorId);
            return;
        }
        for (MajorRequiredCourse mrc : requiredCourses) {
            try {
                Course course = courseRepository.findById(mrc.getCourseId()).orElse(null);
                if (course == null) {
                    continue;
                }
                if (selectionRepository.findByStudentIdAndCourseId(studentId, mrc.getCourseId()).isPresent()) {
                    continue;
                }
                Selection selection = new Selection();
                selection.setStudentId(studentId);
                selection.setCourseId(mrc.getCourseId());
                selectionRepository.save(selection);
                logger.info("为学生 {} 自动分配必修课 {}", studentId, course.getCourseName());
            } catch (Exception e) {
                logger.warn("为学生 {} 自动分配必修课时出错，课程ID={}: {}", studentId, mrc.getCourseId(), e.getMessage());
            }
        }
    }
}
