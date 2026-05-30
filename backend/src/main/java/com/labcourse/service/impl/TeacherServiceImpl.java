package com.labcourse.service.impl;

import com.labcourse.entity.Teacher;
import com.labcourse.repository.TeacherRepository;
import com.labcourse.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherServiceImpl implements TeacherService {

    @Autowired
    private TeacherRepository teacherRepository;

    @Override
    public Teacher login(String teacherNo, String password) {
        return teacherRepository.findByTeacherNoAndPassword(teacherNo, password).orElse(null);
    }

    @Override
    public List<Teacher> list() {
        return teacherRepository.findAll();
    }

    @Override
    public boolean save(Teacher teacher) {
        teacherRepository.save(teacher);
        return true;
    }

    @Override
    public boolean updateById(Teacher teacher) {
        teacherRepository.save(teacher);
        return true;
    }

    @Override
    public boolean removeById(Long id) {
        teacherRepository.deleteById(id);
        return true;
    }
}
