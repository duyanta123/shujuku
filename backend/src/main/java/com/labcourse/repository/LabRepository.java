package com.labcourse.repository;

import com.labcourse.entity.Lab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabRepository extends JpaRepository<Lab, Long> {
    long countByCollegeId(Long collegeId);
}
