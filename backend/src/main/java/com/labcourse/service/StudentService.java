package com.labcourse.service;

import com.labcourse.entity.Student;

import java.util.List;

public interface StudentService {
    Student login(String studentNo, String password);
    List<Student> list(Long collegeId);
    boolean save(Student student);
    boolean updateById(Student student);
    boolean removeById(Long id);
    String resetPassword(Long id);
    boolean changePassword(Long id, String oldPassword, String newPassword);
}
