-- 实验选课系统常用查询SQL
USE lab_course_system;

-- 1. 查询某学生已选课程（连接查询）
SELECT
    s.id AS selection_id,
    c.course_name,
    t.name AS teacher_name,
    l.lab_name,
    l.location,
    c.course_time,
    s.select_time
FROM selection s
JOIN course c ON s.course_id = c.id
JOIN teacher t ON c.teacher_id = t.id
JOIN lab l ON c.lab_id = l.id
WHERE s.student_id = 1
ORDER BY s.select_time DESC;

-- 2. 查询每门课程的选课人数（分组统计）
SELECT
    c.course_name,
    COUNT(s.id) AS selected_count,
    c.max_count,
    CASE
        WHEN COUNT(s.id) >= c.max_count THEN '已满'
        ELSE '可选'
    END AS status
FROM course c
LEFT JOIN selection s ON c.id = s.course_id
GROUP BY c.id, c.course_name, c.max_count
ORDER BY selected_count DESC;

-- 3. 查询某教师的所有课程
SELECT
    c.id,
    c.course_name,
    l.lab_name,
    l.location,
    c.course_time,
    c.max_count,
    COUNT(s.id) AS selected_count
FROM course c
LEFT JOIN lab l ON c.lab_id = l.id
LEFT JOIN selection s ON c.id = s.course_id
WHERE c.teacher_id = 1
GROUP BY c.id, c.course_name, l.lab_name, l.location, c.course_time, c.max_count;

-- 4. 查询某课程的学生名单
SELECT
    s.id,
    s.student_no,
    s.name,
    s.gender,
    m.name AS major,
    sc.select_time,
    score.score,
    a.attendance_status
FROM selection sc
JOIN student s ON sc.student_id = s.id
LEFT JOIN major m ON s.major_id = m.id
LEFT JOIN score ON sc.student_id = score.student_id AND sc.course_id = score.course_id
LEFT JOIN attendance a ON sc.student_id = a.student_id AND sc.course_id = a.course_id
WHERE sc.course_id = 1
ORDER BY s.student_no;

-- 5. 统计各专业学生人数
SELECT
    m.name AS major,
    COUNT(*) AS student_count
FROM student s
LEFT JOIN major m ON s.major_id = m.id
GROUP BY m.name
ORDER BY student_count DESC;

-- 6. 统计各职称教师人数
SELECT
    title,
    COUNT(*) AS teacher_count
FROM teacher
GROUP BY title
ORDER BY teacher_count DESC;

-- 7. 查询有成绩的学生及成绩
SELECT
    s.student_no,
    s.name,
    c.course_name,
    sc.score
FROM score sc
JOIN student s ON sc.student_id = s.id
JOIN course c ON sc.course_id = c.id
ORDER BY s.student_no, c.course_name;

-- 8. 统计每门课的平均成绩
SELECT
    c.course_name,
    AVG(sc.score) AS avg_score,
    COUNT(sc.id) AS student_count
FROM score sc
JOIN course c ON sc.course_id = c.id
GROUP BY c.id, c.course_name
HAVING COUNT(sc.id) > 0
ORDER BY avg_score DESC;

-- 9. 查询考勤记录
SELECT
    s.student_no,
    s.name,
    c.course_name,
    a.attendance_status,
    a.created_at
FROM attendance a
JOIN student s ON a.student_id = s.id
JOIN course c ON a.course_id = c.id
ORDER BY c.course_name, s.student_no;

-- 10. 查询实验室使用情况
SELECT
    l.lab_name,
    l.location,
    l.capacity,
    COUNT(DISTINCT c.id) AS course_count
FROM lab l
LEFT JOIN course c ON l.id = c.lab_id
GROUP BY l.id, l.lab_name, l.location, l.capacity;

-- 存储过程：检查学生是否已选某课程
DELIMITER $$
DROP PROCEDURE IF EXISTS check_course_selection$$
CREATE PROCEDURE check_course_selection(
    IN p_student_id BIGINT,
    IN p_course_id BIGINT
)
BEGIN
    SELECT
        COUNT(*) AS is_selected
    FROM selection
    WHERE student_id = p_student_id AND course_id = p_course_id;
END$$
DELIMITER ;

-- 存储过程：获取学生选课统计
DELIMITER $$
DROP PROCEDURE IF EXISTS get_student_selection_stats$$
CREATE PROCEDURE get_student_selection_stats(
    IN p_student_id BIGINT
)
BEGIN
    SELECT
        COUNT(*) AS total_courses,
        (SELECT COUNT(*) FROM course) AS total_available
    FROM selection
    WHERE student_id = p_student_id;
END$$
DELIMITER ;

-- 存储过程：获取课程选课统计
DELIMITER $$
DROP PROCEDURE IF EXISTS get_course_selection_stats$$
CREATE PROCEDURE get_course_selection_stats(
    IN p_course_id BIGINT
)
BEGIN
    SELECT
        c.course_name,
        COUNT(s.id) AS selected_count,
        c.max_count,
        CASE
            WHEN COUNT(s.id) >= c.max_count THEN '已满'
            ELSE '可选择'
        END AS selection_status
    FROM course c
    LEFT JOIN selection s ON c.id = s.course_id
    WHERE c.id = p_course_id
    GROUP BY c.id, c.course_name, c.max_count;
END$$
DELIMITER ;
