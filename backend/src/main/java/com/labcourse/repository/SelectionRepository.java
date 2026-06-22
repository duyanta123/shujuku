package com.labcourse.repository;

import com.labcourse.entity.Selection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SelectionRepository extends JpaRepository<Selection, Long> {
    Optional<Selection> findByStudentIdAndCourseId(Long studentId, Long courseId);
    List<Selection> findByStudentId(Long studentId);
    List<Selection> findByCourseId(Long courseId);
    long countByCourseId(Long courseId);
    
    @Query("SELECT s FROM Selection s WHERE s.studentId = :studentId AND s.courseId = :courseId")
    Selection checkSelection(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    boolean existsByCourseId(Long courseId);

    boolean existsByStudentId(Long studentId);

    void deleteByStudentId(Long studentId);

    void deleteByCourseId(Long courseId);
}
