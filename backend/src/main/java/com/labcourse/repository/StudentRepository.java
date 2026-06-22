package com.labcourse.repository;

import com.labcourse.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentNo(String studentNo);
    Optional<Student> findByRefreshToken(String refreshToken);
    long countByCollegeId(Long collegeId);
    List<Student> findByCollegeId(Long collegeId);
    long countByMajorId(Long majorId);
}
