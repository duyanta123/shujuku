package com.labcourse.service;

import com.labcourse.entity.Attendance;

import java.util.List;

public interface AttendanceService {
    boolean addAttendance(Long studentId, Long courseId, String status);
    List<Attendance> list();
    boolean save(Attendance attendance);
    boolean updateById(Attendance attendance);
    boolean removeById(Long id);
}
