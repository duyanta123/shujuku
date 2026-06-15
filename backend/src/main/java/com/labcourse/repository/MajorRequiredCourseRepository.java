package com.labcourse.repository;

import com.labcourse.entity.MajorRequiredCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface MajorRequiredCourseRepository extends JpaRepository<MajorRequiredCourse, Long> {
    List<MajorRequiredCourse> findByMajorId(Long majorId);
    Optional<MajorRequiredCourse> findByMajorIdAndCourseId(Long majorId, Long courseId);

    @Transactional
    void deleteByMajorIdAndCourseId(Long majorId, Long courseId);
}