package com.labcourse;

import com.labcourse.entity.*;
import com.labcourse.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class DatabaseConstraintTest {

    @Autowired private CollegeRepository collegeRepository;
    @Autowired private MajorRepository majorRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private LabRepository labRepository;
    @Autowired private SelectionRepository selectionRepository;
    @Autowired private ScoreRepository scoreRepository;
    @Autowired private AttendanceRepository attendanceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    @DisplayName("FK: deleting college with majors is rejected")
    void deleteCollegeWithAssociatedMajors_ShouldBeRejected() {
        TestGraph graph = createGraph("college-major");

        assertThrows(Exception.class, () -> {
            collegeRepository.deleteById(graph.collegeId);
            entityManager.flush();
        });
    }

    @Test
    @Transactional
    @DisplayName("FK: deleting major with students is rejected")
    void deleteMajorWithAssociatedStudents_ShouldBeRejected() {
        TestGraph graph = createGraph("major-student");

        assertThrows(Exception.class, () -> {
            majorRepository.deleteById(graph.majorId);
            entityManager.flush();
        });
    }

    @Test
    @Transactional
    @DisplayName("FK: deleting student with selections is rejected")
    void deleteStudentWithSelections_ShouldBeRejected() {
        TestGraph graph = createGraph("student-selection");
        Selection selection = new Selection();
        selection.setStudentId(graph.studentId);
        selection.setCourseId(graph.courseId);
        selectionRepository.saveAndFlush(selection);

        assertThrows(Exception.class, () -> {
            studentRepository.deleteById(graph.studentId);
            entityManager.flush();
        });
    }

    @Test
    @Transactional
    @DisplayName("FK: deleting student with scores is rejected")
    void deleteStudentWithScores_ShouldBeRejected() {
        TestGraph graph = createGraph("student-score");
        Score score = new Score();
        score.setStudentId(graph.studentId);
        score.setCourseId(graph.courseId);
        score.setScore(new BigDecimal("95.00"));
        scoreRepository.saveAndFlush(score);

        assertThrows(Exception.class, () -> {
            studentRepository.deleteById(graph.studentId);
            entityManager.flush();
        });
    }

    @Test
    @Transactional
    @DisplayName("FK: deleting student with attendance is rejected")
    void deleteStudentWithAttendance_ShouldBeRejected() {
        TestGraph graph = createGraph("student-attendance");
        Attendance attendance = new Attendance();
        attendance.setStudentId(graph.studentId);
        attendance.setCourseId(graph.courseId);
        attendance.setAttendanceStatus(AttendanceStatus.出勤);
        attendance.setAttendanceDate(LocalDate.now());
        attendance.setCheckInTime(LocalDateTime.now());
        attendanceRepository.saveAndFlush(attendance);

        assertThrows(Exception.class, () -> {
            studentRepository.deleteById(graph.studentId);
            entityManager.flush();
        });
    }

    @Test
    @Transactional
    @DisplayName("FK: inserting selection with missing student is rejected")
    void insertSelectionWithNonExistentStudent_ShouldBeRejected() {
        TestGraph graph = createGraph("missing-student");
        Selection selection = new Selection();
        selection.setStudentId(999999999L);
        selection.setCourseId(graph.courseId);

        assertThrows(Exception.class, () -> {
            selectionRepository.save(selection);
            entityManager.flush();
        });
    }

    private TestGraph createGraph(String suffix) {
        String unique = Long.toString(System.nanoTime(), 36);

        College college = new College();
        college.setName("c-college-" + unique);
        college.setStatus("ACTIVE");
        college = collegeRepository.saveAndFlush(college);

        Major major = new Major();
        major.setName("c-major-" + unique);
        major.setCollegeId(college.getId());
        major.setStatus("ACTIVE");
        major = majorRepository.saveAndFlush(major);

        Student student = new Student();
        student.setStudentNo("CS" + System.nanoTime());
        student.setName("c-student-" + unique);
        student.setGender("男");
        student.setCollegeId(college.getId());
        student.setMajorId(major.getId());
        student.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh");
        student = studentRepository.saveAndFlush(student);

        Teacher teacher = new Teacher();
        teacher.setTeacherNo("CT" + System.nanoTime());
        teacher.setName("c-teacher-" + unique);
        teacher.setTitle("Professor");
        teacher.setCollegeId(college.getId());
        teacher.setPassword("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh");
        teacher = teacherRepository.saveAndFlush(teacher);

        Lab lab = new Lab();
        lab.setLabName("c-lab-" + unique);
        lab.setLocation("constraint-room");
        lab.setCapacity(30);
        lab.setCollegeId(college.getId());
        lab = labRepository.saveAndFlush(lab);

        Course course = new Course();
        course.setCourseName("c-course-" + unique);
        course.setTeacherId(teacher.getId());
        course.setLabId(lab.getId());
        course.setCourseTime("周一 1-2节");
        course.setMaxCount(30);
        course.setCollegeId(college.getId());
        course.setCourseType("ELECTIVE");
        course = courseRepository.saveAndFlush(course);

        return new TestGraph(college.getId(), major.getId(), student.getId(), course.getId());
    }

    private record TestGraph(Long collegeId, Long majorId, Long studentId, Long courseId) {}
}
