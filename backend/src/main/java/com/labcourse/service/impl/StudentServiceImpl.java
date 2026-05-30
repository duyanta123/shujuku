package com.labcourse.service.impl;

import com.labcourse.entity.Student;
import com.labcourse.repository.StudentRepository;
import com.labcourse.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Override
    public Student login(String studentNo, String password) {
        return studentRepository.findByStudentNoAndPassword(studentNo, password).orElse(null);
    }

    @Override
    public List<Student> list() {
        return studentRepository.findAll();
    }

    @Override
    public boolean save(Student student) {
        studentRepository.save(student);
        return true;
    }

    @Override
    public boolean updateById(Student student) {
        studentRepository.save(student);
        return true;
    }

    @Override
    public boolean removeById(Long id) {
        studentRepository.deleteById(id);
        return true;
    }
}
