package com.labcourse.service;

import java.util.List;
import java.util.Map;

public interface MajorRequiredCourseService {
    Map<String, Object> bind(Long majorId, Long courseId);
    boolean unbind(Long majorId, Long courseId);
    List<Map<String, Object>> listByMajor(Long majorId);
}