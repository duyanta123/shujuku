package com.labcourse.service;

import com.labcourse.entity.Lab;

import java.util.List;

public interface LabService {
    List<Lab> list();
    boolean save(Lab lab);
    boolean updateById(Lab lab);
    boolean removeById(Long id);
}
