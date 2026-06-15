# Tasks

## 阶段一：数据库 DDL 修复（严重问题 + 优化）

- [x] Task 1: 全局修正外键级联策略 — 所有 `ON UPDATE CASCADE` → `ON UPDATE RESTRICT`
  - [x] SubTask 1.1: 修改 college/major/student/teacher/course/major_required_course/course_teacher 表的外键级联策略（共 12 处 `ON UPDATE CASCADE` → `ON UPDATE RESTRICT`）
  - [x] SubTask 1.2: 为 course.teacher_id 外键补充 `ON DELETE RESTRICT ON UPDATE RESTRICT`
  - [x] SubTask 1.3: 为 selection 表两个外键补充约束名 + 完整级联策略
  - [x] SubTask 1.4: 为 score 表两个外键补充约束名 + 完整级联策略
  - [x] SubTask 1.5: 为 attendance 表两个外键补充约束名 + 完整级联策略

- [x] Task 2: lab 表规范化 — 新增 college_id 外键列
  - [x] SubTask 2.1: lab 表新增 `college_id BIGINT` 列 + 外键约束 + 索引
  - [x] SubTask 2.2: 更新种子数据中 lab 的 college_id 值
  - [x] SubTask 2.3: 为 Lab.java 实体新增 collegeId 字段（@Column + 过渡注释）

- [x] Task 3: 数据完整性约束补充
  - [x] SubTask 3.1: score 表新增 `CHECK (score >= 0 AND score <= 100)` 约束
  - [x] SubTask 3.2: student 表新增 `CHECK (gender IN ('男', '女'))` 约束
  - [x] SubTask 3.3: selection.select_time 改为 `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`
  - [x] SubTask 3.4: attendance.modify_time 从 DATETIME 改为 TIMESTAMP

- [x] Task 4: attendance 表新增 check_in_time 字段
  - [x] SubTask 4.1: 新增 `check_in_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` 列
  - [x] SubTask 4.2: 更新 Attendance.java 实体新增 checkInTime 字段

- [x] Task 5: 新增联合索引
  - [x] SubTask 5.1: `idx_attendance_course_date` 联合索引 `attendance(course_id, attendance_date)`
  - [x] SubTask 5.2: `idx_major_college_status` 联合索引 `major(college_id, status)`
  - [x] SubTask 5.3: `idx_attendance_student_date` 联合索引 `attendance(student_id, attendance_date)`

## 阶段二：数据库编程对象（课程核心考点）

- [x] Task 6: 创建存储过程
  - [x] SubTask 6.1: `proc_check_attendance_status` — 签到状态判定（含重复签到检查）
  - [x] SubTask 6.2: `proc_check_course_selection_conflict` — 选课冲突检查（含 FOR UPDATE 悲观锁、人数上限检查、事务控制）
  - [x] SubTask 6.3: 将两个存储过程添加到 `init_database.sql` 末尾

- [x] Task 7: 创建视图
  - [x] SubTask 7.1: `v_active_college` — 仅 ACTIVE 状态的学院
  - [x] SubTask 7.2: `v_active_major` — 仅 ACTIVE 状态的专业
  - [x] SubTask 7.3: `v_student_course` — 学生-课程-专业-学院关联视图（关联 student + major + selection + course + college）
  - [x] SubTask 7.4: 将三个视图添加到 `init_database.sql` 末尾

- [x] Task 8: 创建触发器
  - [x] SubTask 8.1: 创建 `college_status_log` 日志表
  - [x] SubTask 8.2: 创建 `trigger_college_status_update` 触发器（AFTER UPDATE，ACTIVE→INACTIVE 时记录日志）
  - [x] SubTask 8.3: 将触发器和日志表添加到 `init_database.sql` 末尾

## 阶段三：脚本与文档

- [x] Task 9: 过渡字段淘汰标记
  - [x] SubTask 9.1: 在 `init_database.sql` 中为 student/teacher/course/lab 的 college 过渡字段添加 `@deprecated v2.1` 注释
  - [x] SubTask 9.2: 在 CODE_WIKI.md 中明确淘汰计划时间线和迁移路径

- [x] Task 10: 创建迁移与回滚脚本
  - [x] SubTask 10.1: 创建 `database/migrate_v1_to_v2.sql`（包含所有 DDL 变更 + 数据迁移 UPDATE + 数据验证）
  - [x] SubTask 10.2: 创建 `database/rollback_v2_to_v1.sql`（按逆序回滚所有变更）

- [x] Task 11: 新增数据库级测试
  - [x] SubTask 11.1: `DatabaseConstraintTest.java` — 外键约束测试（删除有子数据的学院/专业/学生/课程被拒绝）
  - [x] SubTask 11.2: `DatabaseUniqueIndexTest.java` — 唯一索引冲突测试（重复学号/工号/用户名/学院名/同学生同课程选课/同学生同课程同天签到插入被拒绝）
  - [x] SubTask 11.3: `DatabaseConcurrencyTest.java` — 并发签到测试（多线程同时签到同一课程同一天，验证唯一约束生效）+ 并发选课测试（多线程选同一课程，验证人数上限 + 存储过程锁）

- [x] Task 12: 更新 CODE_WIKI.md
  - [x] SubTask 12.1: 新增「7.3 数据库索引设计表」章节（索引名、字段、类型、设计理由）
  - [x] SubTask 12.2: 新增「7.4 外键约束明细表」章节（约束名、子表.字段、父表.字段、级联策略、业务含义）
  - [x] SubTask 12.3: 新增「7.5 字段约束表」章节（表名、字段名、类型、长度、非空、默认值、CHECK/ENUM、注释）
  - [x] SubTask 12.4: 新增「7.6 数据库编程对象」章节（存储过程、视图、触发器说明）
  - [x] SubTask 12.5: 更新 ER 图补充 lab.college_id 关系、college_status_log 表
  - [x] SubTask 12.6: 更新数据库表结构说明（attendance.check_in_time、lab.college_id、score CHECK 约束、gender CHECK 约束）
  - [x] SubTask 12.7: 更新版本号为 2.1.0

# Task Dependencies
- Task 2、3、4、5 依赖 Task 1（先修正外键，再改表结构）
- Task 2 和 Task 3 和 Task 4 和 Task 5 可并行（互不依赖）
- Task 6、7、8 可并行（存储过程、视图、触发器互不依赖）
- Task 9 可与 Task 1-8 并行
- Task 10 依赖 Task 1-8（迁移脚本需包含所有变更）
- Task 11 依赖 Task 1-5（测试基于修正后的表结构）
- Task 12 依赖 Task 1-11（文档是所有变更的收尾汇总）