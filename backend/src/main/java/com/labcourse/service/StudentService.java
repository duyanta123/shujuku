package com.labcourse.service;

import com.labcourse.entity.Student;

import java.util.List;

public interface StudentService {
    Student login(String studentNo, String password);
    List<Student> list();
    boolean save(Student student);
    boolean updateById(Student student);
    boolean removeById(Long id);
}
