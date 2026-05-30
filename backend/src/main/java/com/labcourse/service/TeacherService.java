package com.labcourse.service;

import com.labcourse.entity.Teacher;

import java.util.List;

public interface TeacherService {
    Teacher login(String teacherNo, String password);
    List<Teacher> list();
    boolean save(Teacher teacher);
    boolean updateById(Teacher teacher);
    boolean removeById(Long id);
}
