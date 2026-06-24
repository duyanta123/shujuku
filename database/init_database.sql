-- 实验选课系统数据库
-- MySQL 8.0
-- 版本: 2.1 — 新增学院/专业管理层级模型、课程分类、必修课配置
-- 注意：本脚本包含本地演示账号 seed 数据，生产环境不得直接导入。

-- 创建数据库
CREATE DATABASE IF NOT EXISTS lab_course_system
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE lab_course_system;

-- 删除已有的表（如果存在，按外键依赖逆序删除）
DROP TABLE IF EXISTS college_status_log;
DROP TABLE IF EXISTS major_required_course;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS score;
DROP TABLE IF EXISTS selection;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS lab;
DROP TABLE IF EXISTS major;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS college;
DROP TABLE IF EXISTS admin;
DROP TABLE IF EXISTS login_attempts;

-- ============================================================
-- 1. 学院表（新）
-- ============================================================
CREATE TABLE college (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE COMMENT '学院名称',
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE启用/INACTIVE停用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学院表';

-- ============================================================
-- 2. 专业表（新，引用学院）
-- ============================================================
CREATE TABLE major (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '专业名称',
    college_id BIGINT NOT NULL COMMENT '所属学院ID',
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE启用/INACTIVE停用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_major_college FOREIGN KEY (college_id) REFERENCES college(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    UNIQUE KEY uk_college_major (college_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业表';

-- ============================================================
-- 3. 学生表（改造：新增 college_id/major_id 外键）
-- ============================================================
CREATE TABLE student (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_no VARCHAR(20) NOT NULL UNIQUE COMMENT '学号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender VARCHAR(10) COMMENT '性别',
    college_id BIGINT COMMENT '学院ID（新外键）',
    major_id BIGINT COMMENT '专业ID（新外键）',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    refresh_token VARCHAR(512) COMMENT 'Refresh Token',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_college FOREIGN KEY (college_id) REFERENCES college(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_student_major FOREIGN KEY (major_id) REFERENCES major(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_student_gender CHECK (gender IN ('男', '女'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生表';

-- ============================================================
-- 4. 教师表（改造：新增 college_id 外键）
-- ============================================================
CREATE TABLE teacher (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_no VARCHAR(20) NOT NULL UNIQUE COMMENT '工号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    title VARCHAR(50) COMMENT '职称',
    college_id BIGINT COMMENT '学院ID（新外键）',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    refresh_token VARCHAR(512) COMMENT 'Refresh Token',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_teacher_college FOREIGN KEY (college_id) REFERENCES college(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师表';

-- ============================================================
-- 5. 管理员表（不变）
-- ============================================================
CREATE TABLE admin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    refresh_token VARCHAR(512) COMMENT 'Refresh Token',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- ============================================================
-- 6. 实验室表（改造：新增 college_id 外键）
-- ============================================================
CREATE TABLE lab (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lab_name VARCHAR(100) NOT NULL COMMENT '实验室名称',
    location VARCHAR(200) COMMENT '地点',
    capacity INT COMMENT '容量',
    college_id BIGINT COMMENT '学院ID（新外键）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lab_college FOREIGN KEY (college_id) REFERENCES college(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室表';

-- ============================================================
-- 7. 课程表（改造：新增 college_id 外键、course_type 分类）
-- ============================================================
CREATE TABLE course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_name VARCHAR(100) NOT NULL COMMENT '课程名',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    lab_id BIGINT COMMENT '实验室ID',
    course_time VARCHAR(100) COMMENT '上课时间',
    max_count INT DEFAULT 30 COMMENT '最大人数',
    college_id BIGINT COMMENT '学院ID（新外键）',
    course_type ENUM('REQUIRED', 'ELECTIVE') NOT NULL DEFAULT 'ELECTIVE' COMMENT '课程类型：REQUIRED必修/ELECTIVE选修',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_course_lab FOREIGN KEY (lab_id) REFERENCES lab(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_course_college FOREIGN KEY (college_id) REFERENCES college(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- ============================================================
-- 8. 选课表（不变）
-- ============================================================
CREATE TABLE selection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    select_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_selection_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_selection_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    UNIQUE KEY uk_student_course (student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课表';

-- ============================================================
-- 9. 成绩表（不变）
-- ============================================================
CREATE TABLE score (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    score DECIMAL(5,2) COMMENT '成绩',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_score_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_score_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    UNIQUE KEY uk_student_course_score (student_id, course_id),
    CONSTRAINT chk_score_range CHECK (score >= 0 AND score <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';

-- ============================================================
-- 10. 考勤表（不变）
-- ============================================================
CREATE TABLE attendance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    attendance_status ENUM('出勤','请假','缺勤','迟到') COMMENT '出勤状态',
    attendance_date DATE COMMENT '考勤日期',
    check_in_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '实际签到时间',
    modified_by BIGINT COMMENT '修改人ID',
    modify_time TIMESTAMP COMMENT '修改时间',
    modify_reason VARCHAR(200) COMMENT '修改原因',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_attendance_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    UNIQUE KEY uk_student_course_date (student_id, course_id, attendance_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤表';

-- ============================================================
-- 11. 专业-必修课关联表（新）
-- ============================================================
CREATE TABLE major_required_course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    major_id BIGINT NOT NULL COMMENT '专业ID',
    course_id BIGINT NOT NULL COMMENT '必修课ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mrc_major FOREIGN KEY (major_id) REFERENCES major(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_mrc_course FOREIGN KEY (course_id) REFERENCES course(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    UNIQUE KEY uk_major_course (major_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业-必修课关联表';

-- ============================================================
-- 创建索引
-- ============================================================
-- 原有索引
CREATE INDEX idx_course_teacher ON course(teacher_id);
CREATE INDEX idx_course_lab ON course(lab_id);
CREATE INDEX idx_selection_student ON selection(student_id);
CREATE INDEX idx_selection_course ON selection(course_id);
CREATE INDEX idx_score_student ON score(student_id);
CREATE INDEX idx_score_course ON score(course_id);
CREATE INDEX idx_attendance_student ON attendance(student_id);
CREATE INDEX idx_attendance_course ON attendance(course_id);

-- 新增外键列索引
CREATE INDEX idx_major_college ON major(college_id);
CREATE INDEX idx_student_college ON student(college_id);
CREATE INDEX idx_student_major ON student(major_id);
CREATE INDEX idx_course_college ON course(college_id);
CREATE INDEX idx_teacher_college ON teacher(college_id);

-- 联合索引（优化核心查询场景）
CREATE INDEX idx_lab_college ON lab(college_id);
CREATE INDEX idx_attendance_course_date ON attendance(course_id, attendance_date);
CREATE INDEX idx_major_college_status ON major(college_id, status);
CREATE INDEX idx_attendance_student_date ON attendance(student_id, attendance_date);

-- ============================================================
-- 初始数据（密码均已使用BCrypt哈希，强度10）
-- admin: admin@789
-- T001: t001@789, T002: t002@789, T003: t003@789
-- S001: s001@789, S002: s002@789, S003: s003@789, S004: s004@789, S005: s005@789
-- ============================================================

-- 学院数据
INSERT INTO college (name, status) VALUES
('数学与计算机科学学院', 'ACTIVE'),
('物理与电子工程学院', 'ACTIVE'),
('化学与材料科学学院', 'ACTIVE'),
('信息工程学院', 'ACTIVE');

-- 专业数据（归属对应学院）
INSERT INTO major (name, college_id, status) VALUES
-- 数学与计算机科学学院 (id=1)
('计算机科学与技术', 1, 'ACTIVE'),
('软件工程', 1, 'ACTIVE'),
('网络工程', 1, 'ACTIVE'),
-- 物理与电子工程学院 (id=2)
('电子信息工程', 2, 'ACTIVE'),
('通信工程', 2, 'ACTIVE'),
-- 化学与材料科学学院 (id=3)
('应用化学', 3, 'ACTIVE'),
-- 信息工程学院 (id=4)
('信息安全', 4, 'ACTIVE'),
('物联网工程', 4, 'ACTIVE');

-- 管理员数据
INSERT INTO admin (username, password) VALUES
('admin', '$2a$10$g3ua6UFlh2UhJQbGI7KoauWRnaLXzU2I.5TKxCj.caXbbHCxbvpwa');

-- 教师数据（college_id 对应学院）
INSERT INTO teacher (teacher_no, name, title, college_id, password) VALUES
('T001', '张三', '教授', 1, '$2a$10$VFULm.z8fLLyh4H0pBMUV.66DGwnCqSXjwRKLpfYJoOCQq8HaJJR.'),
('T002', '李四', '副教授', 2, '$2a$10$yDx0APFgi27R.CpedD9wGeT4H.M1Izx6g82XeCXNR/.o.VRuSL0l2'),
('T003', '王五', '讲师', 4, '$2a$10$lwx.ABNJ7sAZ7AtpuIevVuzS98f8KYPAfbqEq7MUFdO6KqHqWvv.q');

-- 学生数据（college_id/major_id 对应学院和专业）
INSERT INTO student (student_no, name, gender, college_id, major_id, password) VALUES
('S001', '王小明', '男', 1, 1, '$2a$10$jrkth40eALhwkQwYcSNckuJN.RlUwg1tIytXiF8KKHd/mXNam63Li'),
('S002', '李小红', '女', 1, 2, '$2a$10$3qxRj9Z2eijtmwaA1N4N6.vShkdMn8eXVfHGkoPb6DG2gu4ItWsWG'),
('S003', '张小强', '男', 1, 3, '$2a$10$hkuHkDaFQGfK2I3SSj4NCebQOVxUx3qgyS/bNo0EW.q7WU7Cm1j.i'),
('S004', '刘小芳', '女', 4, 7, '$2a$10$UpXsJ5v0.HwRD0O5UCRvSe7sLYP8/Sf.pOtN24PC3gcSAHrKbob6e'),
('S005', '陈小刚', '男', 4, 8, '$2a$10$aY.jnc9xaogetZfn0GknJeCCy4IixIB5HAIQQovnXxnC18EhkFVFa');

-- 实验室数据
INSERT INTO lab (lab_name, location, capacity, college_id) VALUES
('计算机实验室A', '信息楼101', 40, 1),
('计算机实验室B', '信息楼102', 35, 1),
('软件工程实验室', '信息楼201', 30, 1),
('网络安全实验室', '信息楼301', 25, 4),
('物联网实验室', '信息楼401', 30, 4);

-- 课程数据（含 college_id 和 course_type）
INSERT INTO course (course_name, teacher_id, lab_id, course_time, max_count, college_id, course_type) VALUES
('Java程序设计', 1, 1, '周一 1-2节', 35, 1, 'REQUIRED'),
('数据库原理', 1, 3, '周二 3-4节', 30, 1, 'REQUIRED'),
('Web开发技术', 2, 2, '周三 5-6节', 35, 2, 'ELECTIVE'),
('计算机网络', 2, 4, '周四 7-8节', 30, 2, 'ELECTIVE'),
('软件测试', 3, 3, '周五 1-2节', 30, 4, 'ELECTIVE');

-- 专业-必修课关联：计算机科学与技术专业必修 Java程序设计、数据库原理
INSERT INTO major_required_course (major_id, course_id) VALUES
(1, 1), (1, 2);

-- ============================================================
-- 存量数据迁移脚本（迁移已完成，deprecated 列已删除）
-- 历史升级步骤：原有数据库若存在 college/major 字符串列，先执行以下 UPDATE 进行迁移
-- ============================================================
-- 以下脚本已不再需要（deprecated 列已从表结构中移除）：
-- UPDATE student s
--   LEFT JOIN college c ON s.college = c.name
-- SET s.college_id = c.id
-- WHERE s.college IS NOT NULL AND s.college != '';
-- 
-- UPDATE teacher t
--   LEFT JOIN college c ON t.college = c.name
-- SET t.college_id = c.id
-- WHERE t.college IS NOT NULL AND t.college != '';
-- 
-- UPDATE course co
--   LEFT JOIN college c ON co.college = c.name
-- SET co.college_id = c.id
-- WHERE co.college IS NOT NULL AND co.college != '';

-- 显示数据库结构
SELECT '数据库创建完成！' AS message;
SHOW TABLES;

-- ============================================================
-- 存储过程
-- ============================================================

-- 签到状态判定
DELIMITER $$
DROP PROCEDURE IF EXISTS proc_check_attendance_status$$
CREATE PROCEDURE proc_check_attendance_status(
    IN p_student_id BIGINT,
    IN p_course_id BIGINT,
    IN p_check_time DATETIME,
    OUT p_status VARCHAR(10),
    OUT p_message VARCHAR(200)
)
BEGIN
    DECLARE v_course_time VARCHAR(100);
    DECLARE v_existing_count INT DEFAULT 0;
    DECLARE v_check_date DATE;

    SET v_check_date = DATE(p_check_time);

    -- 获取课程时间
    SELECT course_time INTO v_course_time
    FROM course WHERE id = p_course_id;

    IF v_course_time IS NULL THEN
        SET p_status = 'ERROR';
        SET p_message = '课程时间未设置';
    ELSE
        -- 检查重复签到
        SELECT COUNT(*) INTO v_existing_count
        FROM attendance
        WHERE student_id = p_student_id
          AND course_id = p_course_id
          AND attendance_date = v_check_date;

        IF v_existing_count > 0 THEN
            SET p_status = 'DUPLICATE';
            SET p_message = '今日已签到';
        ELSE
            SET p_status = '出勤';
            SET p_message = '签到成功';
        END IF;
    END IF;
END$$
DELIMITER ;

-- 选课冲突检查（含悲观锁 + 事务）
DELIMITER $$
DROP PROCEDURE IF EXISTS proc_check_course_selection_conflict$$
CREATE PROCEDURE proc_check_course_selection_conflict(
    IN p_student_id BIGINT,
    IN p_course_id BIGINT,
    OUT p_result_code INT,
    OUT p_result_msg VARCHAR(200)
)
BEGIN
    DECLARE v_current_count INT;
    DECLARE v_max_count INT;
    DECLARE v_already_selected INT DEFAULT 0;

    START TRANSACTION;

    -- 悲观锁：锁定课程行
    SELECT max_count INTO v_max_count
    FROM course
    WHERE id = p_course_id
    FOR UPDATE;

    -- 检查是否已选
    SELECT COUNT(*) INTO v_already_selected
    FROM selection
    WHERE student_id = p_student_id AND course_id = p_course_id;

    IF v_already_selected > 0 THEN
        SET p_result_code = 1;
        SET p_result_msg = '已选过该课程';
        ROLLBACK;
    ELSE
        -- 检查人数上限
        SELECT COUNT(*) INTO v_current_count
        FROM selection
        WHERE course_id = p_course_id;

        IF v_current_count >= v_max_count THEN
            SET p_result_code = 2;
            SET p_result_msg = '课程容量已满';
            ROLLBACK;
        ELSE
            -- 插入选课记录
            INSERT INTO selection (student_id, course_id, select_time)
            VALUES (p_student_id, p_course_id, NOW());

            SET p_result_code = 0;
            SET p_result_msg = '选课成功';
            COMMIT;
        END IF;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- 视图
-- ============================================================

-- 视图：仅启用状态的学院
CREATE OR REPLACE VIEW v_active_college AS
SELECT id, name, created_at, updated_at
FROM college
WHERE status = 'ACTIVE';

-- 视图：仅启用状态的专业
CREATE OR REPLACE VIEW v_active_major AS
SELECT id, name, college_id, created_at, updated_at
FROM major
WHERE status = 'ACTIVE';

-- 视图：学生-课程-专业-学院全关联（简化前端多表查询）
CREATE OR REPLACE VIEW v_student_course AS
SELECT
    s.id AS student_id,
    s.student_no,
    s.name AS student_name,
    m.name AS major_name,
    c.name AS college_name,
    co.id AS course_id,
    co.course_name,
    co.course_type,
    co.course_time,
    sel.select_time,
    t.name AS teacher_name
FROM student s
LEFT JOIN major m ON s.major_id = m.id
LEFT JOIN college c ON s.college_id = c.id
LEFT JOIN selection sel ON s.id = sel.student_id
LEFT JOIN course co ON sel.course_id = co.id
LEFT JOIN teacher t ON co.teacher_id = t.id;

-- ============================================================
-- 学院状态变更日志表 + 触发器
-- ============================================================

-- 学院状态变更日志表
CREATE TABLE IF NOT EXISTS college_status_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    college_id BIGINT NOT NULL COMMENT '学院ID',
    old_status VARCHAR(20) NOT NULL COMMENT '变更前状态',
    new_status VARCHAR(20) NOT NULL COMMENT '变更后状态',
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    CONSTRAINT fk_csl_college FOREIGN KEY (college_id) REFERENCES college(id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学院状态变更日志';

-- 触发器：学院状态从ACTIVE变更为INACTIVE时记录日志
DELIMITER $$
DROP TRIGGER IF EXISTS trigger_college_status_update$$
CREATE TRIGGER trigger_college_status_update
    AFTER UPDATE ON college
    FOR EACH ROW
BEGIN
    IF OLD.status = 'ACTIVE' AND NEW.status = 'INACTIVE' THEN
        INSERT INTO college_status_log (college_id, old_status, new_status)
        VALUES (NEW.id, OLD.status, NEW.status);
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- 登录尝试记录表（安全审计修复：持久化登录失败计数）
-- ============================================================
CREATE TABLE login_attempts (
    attempt_key VARCHAR(100) PRIMARY KEY COMMENT '登录标识（如 student:S001）',
    attempts INT NOT NULL DEFAULT 0 COMMENT '失败次数',
    first_attempt_time TIMESTAMP NULL COMMENT '首次失败时间',
    lock_until TIMESTAMP NULL COMMENT '锁定到期时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录尝试记录表';
