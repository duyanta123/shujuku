package com.labcourse.service;

import com.labcourse.entity.Score;
import com.labcourse.repository.ScoreRepository;
import com.labcourse.service.impl.ScoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ScoreServiceImpl 单元测试 — 覆盖成绩管理的 upsert 逻辑
 *
 * 风险行为覆盖：
 * - addScore: 首次录入成绩（INSERT）、覆盖已有成绩（UPDATE）
 * - save/updateById/removeById: 基础 CRUD 路径验证
 */
@SuppressWarnings("null")
class ScoreServiceImplTest {

    private ScoreServiceImpl service;
    private ScoreRepository scoreRepository;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        service = new ScoreServiceImpl();
        scoreRepository = mock(ScoreRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        injectField(service, "scoreRepository", scoreRepository);
        injectField(service, "jdbcTemplate", jdbcTemplate);
    }

    // ================================================================
    // addScore — 首次录入成绩（INSERT 路径）
    // ================================================================

    @Test
    @DisplayName("addScore: 首次录入成绩应创建新记录")
    void addScore_NewRecord_ShouldInsert() {
        Long studentId = 1L;
        Long courseId = 10L;
        BigDecimal scoreValue = new BigDecimal("85.5");

        when(scoreRepository.findByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(Optional.empty());

        boolean result = service.addScore(studentId, courseId, scoreValue);

        assertTrue(result, "首次录入成绩应成功");
        verify(scoreRepository).save(argThat(score ->
                score.getStudentId().equals(studentId) &&
                score.getCourseId().equals(courseId) &&
                score.getScore().compareTo(scoreValue) == 0
        ));
    }

    // ================================================================
    // addScore — 覆盖已有成绩（UPDATE 路径）
    // ================================================================

    @Test
    @DisplayName("addScore: 已有成绩记录应更新为新的分数")
    void addScore_ExistingRecord_ShouldUpdate() {
        Long studentId = 1L;
        Long courseId = 10L;
        BigDecimal oldScore = new BigDecimal("70.0");
        BigDecimal newScore = new BigDecimal("92.0");

        Score existing = new Score();
        existing.setId(100L);
        existing.setStudentId(studentId);
        existing.setCourseId(courseId);
        existing.setScore(oldScore);

        when(scoreRepository.findByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(Optional.of(existing));

        boolean result = service.addScore(studentId, courseId, newScore);

        assertTrue(result, "覆盖已有成绩应成功");
        assertEquals(newScore, existing.getScore(), "分数应更新为新值");
        verify(scoreRepository).save(existing);
        // 确认没有创建新记录
        verify(scoreRepository, never()).save(argThat(score -> score.getId() == null));
    }

    @Test
    @DisplayName("addScore: 覆盖为相同分数也应正确更新")
    void addScore_SameScore_ShouldStillSave() {
        Long studentId = 1L;
        Long courseId = 10L;
        BigDecimal sameScore = new BigDecimal("85.0");

        Score existing = new Score();
        existing.setId(100L);
        existing.setStudentId(studentId);
        existing.setCourseId(courseId);
        existing.setScore(sameScore);

        when(scoreRepository.findByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(Optional.of(existing));

        boolean result = service.addScore(studentId, courseId, sameScore);

        assertTrue(result, "覆盖为相同分数也应成功");
        verify(scoreRepository).save(existing);
    }

    // ================================================================
    // save — 直接保存
    // ================================================================

    @Test
    @DisplayName("save: 直接保存 Score 实体")
    void save_ShouldPersistScore() {
        Score score = new Score();
        score.setStudentId(2L);
        score.setCourseId(20L);
        score.setScore(new BigDecimal("78.0"));

        boolean result = service.save(score);

        assertTrue(result);
        verify(scoreRepository).save(score);
    }

    // ================================================================
    // updateById — 按 ID 更新
    // ================================================================

    @Test
    @DisplayName("updateById: 应通过 save 完成更新")
    void updateById_ShouldCallSave() {
        Score score = new Score();
        score.setId(200L);
        score.setStudentId(3L);
        score.setCourseId(30L);
        score.setScore(new BigDecimal("95.0"));

        boolean result = service.updateById(score);

        assertTrue(result);
        verify(scoreRepository).save(score);
    }

    // ================================================================
    // removeById — 按 ID 删除
    // ================================================================

    @Test
    @DisplayName("removeById: 应调用 deleteById")
    void removeById_ShouldCallDelete() {
        boolean result = service.removeById(300L);

        assertTrue(result);
        verify(scoreRepository).deleteById(300L);
    }

    // ================================================================
    // list — 查询全表
    // ================================================================

    @Test
    @DisplayName("list: 应返回所有成绩记录")
    void list_ShouldReturnAll() {
        when(scoreRepository.findAll()).thenReturn(java.util.List.of(new Score(), new Score()));

        var result = service.list();

        assertEquals(2, result.size());
        verify(scoreRepository).findAll();
    }

    // ================================================================
    // getScoresByCourse — 新增方法（commit 4274e4f）
    // ================================================================

    @Test
    @DisplayName("getScoresByCourse: 应返回学生成绩列表含学院和专业信息")
    void getScoresByCourse_ShouldReturnStudentScores() {
        Long courseId = 10L;
        Map<String, Object> row1 = new java.util.HashMap<>();
        row1.put("student_id", 1L);
        row1.put("student_no", "S001");
        row1.put("studentNo", "S001");
        row1.put("name", "张三");
        row1.put("gender", "男");
        row1.put("major", "计算机科学");
        row1.put("college", "信息学院");
        row1.put("score", new BigDecimal("85.5"));
        row1.put("course_id", courseId);
        row1.put("courseId", courseId);

        Map<String, Object> row2 = new java.util.HashMap<>();
        row2.put("student_id", 2L);
        row2.put("student_no", "S002");
        row2.put("studentNo", "S002");
        row2.put("name", "李四");
        row2.put("gender", "女");
        row2.put("major", "软件工程");
        row2.put("college", "信息学院");
        row2.put("score", null);
        row2.put("course_id", courseId);
        row2.put("courseId", courseId);

        when(jdbcTemplate.queryForList(anyString(), eq(courseId)))
                .thenReturn(List.of(row1, row2));

        List<Map<String, Object>> result = service.getScoresByCourse(courseId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("张三", result.get(0).get("name"));
        assertEquals(new BigDecimal("85.5"), result.get(0).get("score"));
        assertEquals("计算机科学", result.get(0).get("major"));
        assertEquals("信息学院", result.get(0).get("college"));
        assertNull(result.get(1).get("score"), "未录入成绩的学生 score 应为 null");
        verify(jdbcTemplate).queryForList(anyString(), eq(courseId));
    }

    @Test
    @DisplayName("getScoresByCourse: 无选课学生时应返回空列表")
    void getScoresByCourse_NoStudents_ShouldReturnEmpty() {
        Long courseId = 999L;
        when(jdbcTemplate.queryForList(anyString(), eq(courseId)))
                .thenReturn(List.of());

        List<Map<String, Object>> result = service.getScoresByCourse(courseId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ===== 工具方法 =====

    private static void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            fail("无法注入 " + fieldName + ": " + e.getMessage());
        }
    }
}