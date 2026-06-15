package com.labcourse.service;

import com.labcourse.entity.Major;

import java.util.List;
import java.util.Map;

public interface MajorService {
    Map<String, Object> list(String name, Long collegeId, String status, int page, int size, String sortBy, String sortDir);
    List<Major> listByCollegeId(Long collegeId);
    Major getById(Long id);
    boolean save(Major major);
    boolean update(Major major);
    Map<String, Object> delete(Long id);
}