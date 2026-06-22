package com.labcourse.service;

import com.labcourse.entity.Score;
import com.labcourse.repository.ScoreRepository;
import com.labcourse.service.impl.ScoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

    @BeforeEach
    void setUp() {
        service = new ScoreServiceImpl();
        scoreRepository = mock(ScoreRepository.class);
        injectField(service, "scoreRepository", scoreRepository);
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