package com.labcourse.service;

import com.labcourse.entity.College;

import java.util.Map;

public interface CollegeService {
    Map<String, Object> list(String name, String status, int page, int size, String sortBy, String sortDir);
    College getById(Long id);
    boolean save(College college);
    boolean update(College college);
    Map<String, Object> delete(Long id);
}