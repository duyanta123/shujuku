package com.labcourse.repository;

import com.labcourse.entity.Major;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {
    List<Major> findByCollegeId(Long collegeId);
    Page<Major> findByCollegeId(Long collegeId, Pageable pageable);
    Optional<Major> findByCollegeIdAndName(Long collegeId, String name);
    List<Major> findByCollegeIdAndStatus(Long collegeId, String status);
    long countByCollegeId(Long collegeId);
    Page<Major> findByNameContaining(String name, Pageable pageable);
    Page<Major> findByNameContainingAndCollegeId(String name, Long collegeId, Pageable pageable);
}