# 数据库层面修复与优化 — 全面审阅报告

## Why
本项目作为数据库课程大作业，当前数据库层在约束、索引、编程对象（存储过程/视图/触发器）方面存在显著不足，部分设计有工程化缺陷，无法充分体现数据库课程核心知识点的掌握。需系统性地修复缺陷、补充缺失模块、完善文档。

---

## 一、问题清单（按严重程度分级）

### 【严重】问题 1：外键级联策略不合理
**现状**: 所有外键使用 `ON DELETE RESTRICT + ON UPDATE CASCADE`
**问题**: 主键均为自增 BIGINT，实际场景中从不更新主键值，`ON UPDATE CASCADE` 无任何实际意义，反而增加数据库校验开销。selection、score、attendance 表的外键未显式声明级联策略（仅写 `FOREIGN KEY (x) REFERENCES y(id)`），默认行为不清。
**课程考点**: 外键约束、参照完整性、级联策略语义理解

### 【严重】问题 2：selection 表外键缺少级联策略声明
**现状**: `FOREIGN KEY (student_id) REFERENCES student(id)` — 未指定 ON DELETE
**问题**: MySQL 默认外键行为依赖存储引擎和版本，不显式声明可能导致行为不一致。且语义上选课记录应随学生删除而受限（RESTRICT），不明确声明无法体现设计意图。
**课程考点**: 外键约束完整定义

### 【严重】问题 3：score 表外键缺少级联策略声明
**现状**: 同上，`FOREIGN KEY (student_id) REFERENCES student(id)` 和 `FOREIGN KEY (course_id) REFERENCES course(id)` 均未指定级联策略
**问题**: 成绩记录比选课更重要，删除学生时应先检查是否有成绩记录，必须显式声明 RESTRICT。
**课程考点**: 参照完整性、数据保护

### 【严重】问题 4：attendance 表外键缺少级联策略声明
**现状**: 同上，两个外键均未指定级联策略
**问题**: 考勤记录是核心业务数据，必须受外键约束保护。
**课程考点**: 参照完整性

### 【严重】问题 5：course 表 teacher_id 外键缺少级联策略
**现状**: `CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id)` — 仅有约束名，未写 ON DELETE / ON UPDATE
**问题**: 与其他外键（如 fk_course_college）写法不一致，缺少级联策略声明。
**课程考点**: 外键约束一致性

### 【严重】问题 6：软删除与外键约束的语义冲突
**现状**: college/major 使用 status=INACTIVE 实现软删除，但外键约束为 `ON DELETE RESTRICT`，仅阻止物理 DELETE。Java 代码中通过 `countByCollegeId()` 手动检查关联数据。
**问题**: 
- 数据库层无法感知"逻辑删除"语义，无法通过约束自动联动
- 若有人绕过 Java 代码直接 SQL UPDATE college SET status='INACTIVE'，外键约束不会阻止
- 查询学生/教师/课程时，需在业务代码中手动过滤 status='ACTIVE'，容易遗漏
**课程考点**: 约束设计、软删除实现模式、数据完整性

### 【严重】问题 7：选课并发控制缺失
**现状**: 仅签到流程使用 `SELECT ... FOR UPDATE` 悲观锁，选课冲突检查（同一学生重复选课、课程人数上限）完全依赖 Java 代码逻辑。
**问题**: 高并发选课时，多个请求同时通过"人数检查"再同时插入，导致超选。`uk_student_course` 唯一索引可防止重复选课，但 `max_count` 超限无法阻止。
**课程考点**: 事务、锁机制、并发控制

### 【严重】问题 8：数据库编程能力体现不足
**现状**: queries.sql 中有 3 个简单存储过程（仅查询功能），无视图、无触发器、无复杂业务逻辑存储过程。
**问题**: 数据库课程核心考核点（存储过程、视图、触发器）严重缺失，无法体现对数据库编程的掌握。签到状态判定、选课冲突检查等核心业务逻辑完全在 Java 代码中实现。
**课程考点**: 存储过程、视图、触发器、数据库编程

---

### 【优化】问题 9：联合索引缺失导致查询性能隐患
**现状**: 仅有单列索引，缺少联合索引。核心查询场景如下：
- 教师按课程+日期查考勤: `WHERE course_id=? AND attendance_date=?` → 无联合索引，只能用一个单列索引
- 按学院+状态查专业: `WHERE college_id=? AND status=?` → 无联合索引
- 按学生+日期查考勤: `WHERE student_id=? AND attendance_date=?` → 无联合索引
**课程考点**: 索引设计、联合索引、查询优化、最左前缀原则

### 【优化】问题 10：lab 表 college 字段仍为字符串
**现状**: lab 表只有 `college VARCHAR(100)` 字符串字段，无 college_id 外键。
**问题**: 与其他表（student/teacher/course）的改造不一致，学院数据变更时 lab 表无法自动同步，且无法利用外键约束保护数据完整性。
**课程考点**: 数据库规范化、外键约束

### 【优化】问题 11：过渡字段无淘汰计划
**现状**: student/teacher/course 表同时存在 `college VARCHAR(100)` 和 `college_id BIGINT` 两套字段，仅注释"过渡字段，逐步迁移至 college_id"，无具体时间线和迁移方案。
**问题**: 数据冗余、潜在不一致（college 字符串与 college_id 指向的 college.name 可能不同步）、查询时需决定用哪个字段。
**课程考点**: 数据库重构、数据迁移、规范化

### 【优化】问题 12：时间字段类型不一致
**现状**: `created_at/updated_at` 使用 TIMESTAMP，但 `attendance.modify_time` 和 `selection.select_time` 使用 DATETIME。
**问题**: TIMESTAMP 自动受时区影响（存储时转 UTC，读取时转回），DATETIME 存什么读什么。二者混用容易导致时区错乱。
**课程考点**: 数据类型选择、时区处理

### 【优化】问题 13：attendance 表缺少 check_in_time 字段
**现状**: 签到时间仅记录在 `created_at` 中，与"创建时间"语义混淆。`created_at` 应表示记录创建时间，实际签到时间应单独存储。
**问题**: 考勤审计时无法区分"记录创建时间"和"实际签到时间"，且 `created_at` 可能被其他操作触发更新（如修改考勤状态）。
**课程考点**: 数据完整性、语义清晰性

### 【优化】问题 14：score 表缺少成绩约束
**现状**: `score DECIMAL(5,2)` 无 CHECK 约束限制范围。
**问题**: 可插入负数或超过 100 的成绩，数据完整性依赖 Java 代码校验。
**课程考点**: CHECK 约束、域完整性

### 【优化】问题 15：gender 字段未约束
**现状**: `gender VARCHAR(10)` 无约束，可插入任意值。
**问题**: 数据完整性依赖前端/Java 校验，数据库层无保护。
**课程考点**: CHECK 约束或 ENUM 类型

---

### 【文档】问题 16：CODE_WIKI.md 缺少数据库设计核心章节
**现状**: 文档仅有表结构说明和索引列表，缺少：
- 数据库索引设计表（索引名、字段、类型、设计理由）
- 外键约束明细表（外键名、子表字段、父表、级联策略、业务含义）
- 字段约束表（字段名、类型、长度、非空、默认值、CHECK 约束、注释）
- ER 图缺少关系基数标注和详细字段
**课程考点**: 数据库文档规范、ER 图设计

### 【文档】问题 17：缺少数据库级测试
**现状**: 仅有 Java 单元测试和 E2E 测试，缺少数据库层面的专项测试。
**问题**: 外键约束是否生效、唯一索引是否阻止重复数据、并发场景下数据是否一致，均未验证。
**课程考点**: 数据库测试、数据完整性验证

### 【文档】问题 18：缺少迁移和回滚脚本
**现状**: init_database.sql 中注释了存量迁移 SQL，但无独立的迁移脚本和回滚脚本。
**问题**: 无法体现数据库版本升级能力和可回滚设计。
**课程考点**: 数据库版本管理、迁移脚本

---

## 二、逐条整改方案

### 整改 1：外键级联策略全局修正
**修改文件**: `database/init_database.sql`
**方案**: 所有外键统一改为 `ON DELETE RESTRICT ON UPDATE RESTRICT`
**原因**: 主键自增不更新，ON UPDATE CASCADE 无意义；ON DELETE RESTRICT 保护数据不被级联删除。

```sql
-- 修正前
CONSTRAINT fk_major_college FOREIGN KEY (college_id) REFERENCES college(id)
    ON DELETE RESTRICT ON UPDATE CASCADE

-- 修正后
CONSTRAINT fk_major_college FOREIGN KEY (college_id) REFERENCES college(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
```

涉及所有外键（共 12 处）：
- fk_major_college (major.college_id → college.id)
- fk_student_college (student.college_id → college.id)
- fk_student_major (student.major_id → major.id)
- fk_teacher_college (teacher.college_id → college.id)
- fk_course_teacher (course.teacher_id → teacher.id)
- fk_course_lab (course.lab_id → lab.id)
- fk_course_college (course.college_id → college.id)
- fk_mrc_major (major_required_course.major_id → major.id)
- fk_mrc_course (major_required_course.course_id → course.id)
- fk_ct_course (course_teacher.course_id → course.id)
- fk_ct_teacher (course_teacher.teacher_id → teacher.id)
- selection 表两个外键、score 表两个外键、attendance 表两个外键（当前无约束名）

### 整改 2-4：selection/score/attendance 外键补全
**修改文件**: `database/init_database.sql`
**方案**: 为三个表的外键补充约束名和级联策略

```sql
-- selection 表
CONSTRAINT fk_selection_student FOREIGN KEY (student_id) REFERENCES student(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
CONSTRAINT fk_selection_course FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT

-- score 表
CONSTRAINT fk_score_student FOREIGN KEY (student_id) REFERENCES student(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
CONSTRAINT fk_score_course FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT

-- attendance 表
CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES student(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
CONSTRAINT fk_attendance_course FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
```

### 整改 5：course.teacher_id 外键补全级联策略
**修改文件**: `database/init_database.sql`
```sql
-- 修正后
CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
```

### 整改 6：软删除配套视图 + 触发器
**修改文件**: `database/init_database.sql`
**方案**: 
1. 创建 `v_active_college` 和 `v_active_major` 视图，仅返回 ACTIVE 状态的记录
2. 创建 `trigger_college_status_update` 触发器，当学院状态变更为 INACTIVE 时记录日志

```sql
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

-- 日志表
CREATE TABLE college_status_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    college_id BIGINT NOT NULL,
    old_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (college_id) REFERENCES college(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学院状态变更日志';

-- 触发器
DELIMITER $$
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
```

### 整改 7：选课并发控制 — 存储过程 + 悲观锁
**修改文件**: `database/init_database.sql` + 后端 SelectionServiceImpl
**方案**: 创建 `proc_check_course_selection_conflict` 存储过程，在数据库层使用 `SELECT ... FOR UPDATE` 锁定课程行，原子性地检查人数上限并插入选课记录。

```sql
DELIMITER $$
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

    -- 开始事务
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
```

### 整改 8：签到状态判定存储过程
**修改文件**: `database/init_database.sql`
**方案**: 创建 `proc_check_attendance_status` 存储过程，将签到状态判定逻辑从 Java 代码迁移到数据库层

```sql
DELIMITER $$
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
    DECLARE v_target_time TIME;

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
            -- 解析课程时间并判定状态
            -- 使用节次-时间映射表判定
            SET p_status = '出勤';  -- 默认出勤，实际判定逻辑在 Java 层也可调用此存储过程
            SET p_message = '签到成功';
        END IF;
    END IF;
END$$
DELIMITER ;
```

### 整改 9：新增联合索引
**修改文件**: `database/init_database.sql`
**方案**: 为核心查询场景创建联合索引

```sql
-- 教师按课程+日期查考勤
CREATE INDEX idx_attendance_course_date ON attendance(course_id, attendance_date);

-- 按学院+状态查专业（筛选启用专业）
CREATE INDEX idx_major_college_status ON major(college_id, status);

-- 按学生+日期查考勤历史
CREATE INDEX idx_attendance_student_date ON attendance(student_id, attendance_date);
```

### 整改 10：lab 表新增 college_id 外键
**修改文件**: `database/init_database.sql`
**方案**: lab 表新增 `college_id BIGINT` 外键列，保留 `college VARCHAR(100)` 作为过渡字段

```sql
ALTER TABLE lab ADD COLUMN college_id BIGINT COMMENT '学院ID（新外键）';
ALTER TABLE lab ADD CONSTRAINT fk_lab_college 
    FOREIGN KEY (college_id) REFERENCES college(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT;
CREATE INDEX idx_lab_college ON lab(college_id);
```

### 整改 11：过渡字段淘汰计划
**修改文件**: `database/init_database.sql` + CODE_WIKI.md
**方案**:
- 在 SQL 中为过渡字段添加 `-- @deprecated 计划 v2.1 删除` 注释
- 在 CODE_WIKI.md 中明确：
  - v2.0: college 字符串 + college_id 外键并存（过渡期）
  - v2.1: 删除 college 字符串字段，全面使用外键
  - 迁移路径: 执行 `migrate_v1_to_v2.sql` 中注释的 UPDATE 语句，确认所有 college_id 非 NULL 后删除旧字段

### 整改 12：统一时间字段类型
**修改文件**: `database/init_database.sql`
**方案**: `attendance.modify_time` 和 `selection.select_time` 从 DATETIME 改为 TIMESTAMP
```sql
-- selection 表
select_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '选课时间'

-- attendance 表
modify_time TIMESTAMP COMMENT '修改时间'
```

### 整改 13：attendance 新增 check_in_time 字段
**修改文件**: `database/init_database.sql`
**方案**: 新增 `check_in_time TIMESTAMP NOT NULL COMMENT '实际签到时间'`，将签到时间与记录创建时间分离
```sql
ALTER TABLE attendance ADD COLUMN check_in_time TIMESTAMP NOT NULL 
    DEFAULT CURRENT_TIMESTAMP COMMENT '实际签到时间'
    AFTER attendance_date;
```

### 整改 14：score 表添加 CHECK 约束
**修改文件**: `database/init_database.sql`
**方案**: 
```sql
ALTER TABLE score ADD CONSTRAINT chk_score_range 
    CHECK (score >= 0 AND score <= 100);
```

### 整改 15：gender 字段添加 CHECK 约束
**修改文件**: `database/init_database.sql`
**方案**:
```sql
ALTER TABLE student ADD CONSTRAINT chk_gender 
    CHECK (gender IN ('男', '女'));
```

### 整改 16：CODE_WIKI.md 新增数据库设计章节
**修改文件**: `CODE_WIKI.md`
**方案**: 在「7. 数据库设计」章节下新增：
- 7.3 数据库索引设计表
- 7.4 外键约束明细表
- 7.5 字段约束表
- 7.6 数据库编程对象（存储过程/视图/触发器）
- 更新版本号为 2.1.0

### 整改 17：新增数据库级测试
**修改文件**: `backend/src/test/java/com/labcourse/`
**方案**: 新增三个测试类
- `DatabaseConstraintTest.java`: 外键约束测试（删除有子数据的父记录被拒绝）
- `DatabaseUniqueIndexTest.java`: 唯一索引冲突测试（重复数据插入被拒绝）
- `DatabaseConcurrencyTest.java`: 并发测试（多线程同时签到/选课，验证数据一致性）

### 整改 18：新增迁移和回滚脚本
**修改文件**: `database/migrate_v1_to_v2.sql` + `database/rollback_v2_to_v1.sql`
**方案**: 
- migrate_v1_to_v2.sql: 包含所有 DDL 变更（新增字段、外键、索引、约束、存储过程、视图、触发器）+ 数据迁移
- rollback_v2_to_v1.sql: 按逆序回滚所有变更

---

## 三、Impact
- Affected specs: 无现有 spec 冲突
- Affected code: 
  - `database/init_database.sql` — 主要修改文件
  - `database/queries.sql` — 需更新查询以使用新视图
  - `database/migrate_v1_to_v2.sql` — 新增
  - `database/rollback_v2_to_v1.sql` — 新增
  - `CODE_WIKI.md` — 新增数据库设计章节
  - `backend/src/main/java/com/labcourse/entity/Lab.java` — 新增 collegeId 字段
  - `backend/src/main/java/com/labcourse/service/impl/SelectionServiceImpl.java` — 可选：改用存储过程
  - `backend/src/test/java/com/labcourse/` — 新增 3 个测试类

---

## 四、ADDED Requirements

### Requirement: 外键约束策略修正
系统 SHALL 将所有外键的 `ON UPDATE CASCADE` 改为 `ON UPDATE RESTRICT`，并为 selection、score、attendance 表的外键补充约束名和完整的级联策略声明。

#### Scenario: 所有外键使用 RESTRICT 级联
- **WHEN** 数据库初始化脚本执行
- **THEN** 所有外键约束均为 `ON DELETE RESTRICT ON UPDATE RESTRICT`，且均有唯一约束名

### Requirement: 软删除配套视图与触发器
系统 SHALL 创建 `v_active_college`、`v_active_major` 视图仅查询启用状态的记录，并创建 `trigger_college_status_update` 触发器记录学院状态变更日志。

#### Scenario: 学院软删除时触发日志记录
- **WHEN** 执行 `UPDATE college SET status='INACTIVE' WHERE id=1`
- **THEN** `college_status_log` 表中自动插入一条变更记录

### Requirement: 选课并发控制存储过程
系统 SHALL 创建 `proc_check_course_selection_conflict` 存储过程，使用 `SELECT ... FOR UPDATE` 悲观锁机制，原子性地检查选课冲突并插入记录。

#### Scenario: 并发选课不超限
- **WHEN** 100 个学生同时选修同一门容量为 30 的课程
- **THEN** 仅前 30 个请求成功，其余返回"容量已满"

### Requirement: 签到状态判定存储过程
系统 SHALL 创建 `proc_check_attendance_status` 存储过程，在数据库层实现签到状态判定逻辑。

### Requirement: 联合索引
系统 SHALL 新增 `idx_attendance_course_date(course_id, attendance_date)`、`idx_major_college_status(college_id, status)`、`idx_attendance_student_date(student_id, attendance_date)` 联合索引。

### Requirement: lab 表规范化
系统 SHALL 为 lab 表新增 `college_id BIGINT` 外键列，关联 college 表，保留原 `college VARCHAR(100)` 作为过渡字段。

### Requirement: 数据完整性约束
系统 SHALL 为 `score.score` 添加 `CHECK (score >= 0 AND score <= 100)` 约束，为 `student.gender` 添加 `CHECK (gender IN ('男', '女'))` 约束。

### Requirement: 字段类型统一
系统 SHALL 将 `selection.select_time` 和 `attendance.modify_time` 从 DATETIME 改为 TIMESTAMP，统一时间字段类型。

### Requirement: attendance 表新增 check_in_time
系统 SHALL 为 attendance 表新增 `check_in_time TIMESTAMP NOT NULL` 字段，与 `created_at` 分离。

### Requirement: 过渡字段淘汰计划
系统 SHALL 在 SQL 中为过渡字段添加 `@deprecated` 注释，并在 CODE_WIKI.md 中明确 v2.1 删除计划。

### Requirement: 数据迁移与回滚脚本
系统 SHALL 创建 `migrate_v1_to_v2.sql` 和 `rollback_v2_to_v1.sql`。

### Requirement: 数据库级测试
系统 SHALL 新增 `DatabaseConstraintTest`、`DatabaseUniqueIndexTest`、`DatabaseConcurrencyTest` 三个测试类。

## MODIFIED Requirements

### Requirement: CODE_WIKI 文档补充
CODE_WIKI.md SHALL 新增「数据库索引设计表」、「外键约束明细表」、「字段约束表」、「数据库编程对象」章节，更新 ER 图，并更新版本号为 2.1.0。