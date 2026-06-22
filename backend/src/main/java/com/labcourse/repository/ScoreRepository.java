package com.labcourse.repository;

import com.labcourse.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {
    Optional<Score> findByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByStudentId(Long studentId);

    void deleteByStudentId(Long studentId);

    void deleteByCourseId(Long courseId);
}
