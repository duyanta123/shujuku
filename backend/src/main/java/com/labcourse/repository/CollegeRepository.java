package com.labcourse.repository;

import com.labcourse.entity.College;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollegeRepository extends JpaRepository<College, Long> {
    List<College> findByStatus(String status);
    Page<College> findByStatus(String status, Pageable pageable);
    Optional<College> findByName(String name);
    Page<College> findByNameContaining(String name, Pageable pageable);
    Page<College> findByNameContainingAndStatus(String name, String status, Pageable pageable);
}