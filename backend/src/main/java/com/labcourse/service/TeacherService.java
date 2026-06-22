package com.labcourse.service;

import com.labcourse.entity.Teacher;

import java.util.List;

public interface TeacherService {
    Teacher login(String teacherNo, String password);
    List<Teacher> list(Long collegeId);
    boolean save(Teacher teacher);
    boolean updateById(Teacher teacher);
    boolean removeById(Long id);
    String resetPassword(Long id);
    boolean changePassword(Long id, String oldPassword, String newPassword);
}
