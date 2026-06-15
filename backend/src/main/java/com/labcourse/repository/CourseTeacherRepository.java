package com.labcourse.repository;

import com.labcourse.entity.CourseTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, Long> {
    Optional<CourseTeacher> findByTeacherId(Long teacherId);
    List<CourseTeacher> findByCourseId(Long courseId);

    @Transactional
    void deleteByTeacherId(Long teacherId);
}