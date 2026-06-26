package com.labcourse.service.impl;

import com.labcourse.entity.Score;
import com.labcourse.repository.ScoreRepository;
import com.labcourse.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("null")
public class ScoreServiceImpl implements ScoreService {

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public boolean addScore(Long studentId, Long courseId, BigDecimal score) {
        Score existingScore = scoreRepository.findByStudentIdAndCourseId(studentId, courseId).orElse(null);

        if (existingScore != null) {
            existingScore.setScore(score);
            scoreRepository.save(existingScore);
        } else {
            Score newScore = new Score();
            newScore.setStudentId(studentId);
            newScore.setCourseId(courseId);
            newScore.setScore(score);
            scoreRepository.save(newScore);
        }
        return true;
    }

    @Override
    public List<Map<String, Object>> getScoresByCourse(Long courseId) {
        String sql = """
            SELECT
                s.id AS student_id,
                s.student_no,
                s.student_no AS studentNo,
                s.name,
                s.gender,
                m.name AS major,
                c.name AS college,
                sc.score,
                sel.course_id,
                sel.course_id AS courseId
            FROM selection sel
            JOIN student s ON sel.student_id = s.id
            LEFT JOIN major m ON s.major_id = m.id
            LEFT JOIN college c ON s.college_id = c.id
            LEFT JOIN score sc ON sc.student_id = s.id AND sc.course_id = sel.course_id
            WHERE sel.course_id = ?
            ORDER BY s.student_no
            """;
        return jdbcTemplate.queryForList(sql, courseId);
    }

    @Override
    public List<Score> list() {
        return scoreRepository.findAll();
    }

    @Override
    public boolean save(Score score) {
        scoreRepository.save(score);
        return true;
    }

    @Override
    public boolean updateById(Score score) {
        scoreRepository.save(score);
        return true;
    }

    @Override
    public boolean removeById(Long id) {
        scoreRepository.deleteById(id);
        return true;
    }
}
