package com.labcourse.service.impl;

import com.labcourse.entity.Attendance;
import com.labcourse.repository.AttendanceRepository;
import com.labcourse.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Override
    public boolean addAttendance(Long studentId, Long courseId, String status) {
        Attendance existing = attendanceRepository.findByStudentIdAndCourseId(studentId, courseId).orElse(null);

        if (existing != null) {
            existing.setAttendanceStatus(status);
            attendanceRepository.save(existing);
        } else {
            Attendance newAttendance = new Attendance();
            newAttendance.setStudentId(studentId);
            newAttendance.setCourseId(courseId);
            newAttendance.setAttendanceStatus(status);
            attendanceRepository.save(newAttendance);
        }
        return true;
    }

    @Override
    public List<Attendance> list() {
        return attendanceRepository.findAll();
    }

    @Override
    public boolean save(Attendance attendance) {
        attendanceRepository.save(attendance);
        return true;
    }

    @Override
    public boolean updateById(Attendance attendance) {
        attendanceRepository.save(attendance);
        return true;
    }

    @Override
    public boolean removeById(Long id) {
        attendanceRepository.deleteById(id);
        return true;
    }
}
