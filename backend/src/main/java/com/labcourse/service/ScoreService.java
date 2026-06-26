package com.labcourse.service;

import com.labcourse.entity.Score;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ScoreService {
    boolean addScore(Long studentId, Long courseId, BigDecimal score);
    List<Map<String, Object>> getScoresByCourse(Long courseId);
    List<Score> list();
    boolean save(Score score);
    boolean updateById(Score score);
    boolean removeById(Long id);
}
