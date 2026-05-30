# 实验选课系统

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.0-green" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.4-blue" alt="Vue">
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL">
  <img src="https://img.shields.io/badge/Vite-5.0-purple" alt="Vite">
</p>

<p align="center">
  <strong>基于 Spring Boot + Vue 3 的实验选课管理系统</strong>
</p>

---

## 📋 目录

- [项目介绍](#项目介绍)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [功能模块](#功能模块)
- [数据库设计](#数据库设计)
- [快速开始](#快速开始)
- [项目启动](#项目启动)
- [API接口文档](#api接口文档)
- [测试报告](#测试报告)
- [开发指南](#开发指南)
- [常见问题](#常见问题)

---

## 项目介绍

### 项目概述

实验选课系统是一个面向高校实验室课程管理的完整Web应用系统，采用前后端分离架构，实现学生选课、教师考勤与成绩管理、管理员系统管理等核心功能。

### 项目特点

- 🎓 **三层架构设计**：清晰的表示层、业务逻辑层、数据访问层分离
- 🔄 **前后端分离**：现代化的SPA应用架构
- 📱 **响应式设计**：适配多种终端设备
- 🔒 **权限管理**：基于角色的访问控制（RBAC）
- 💾 **数据一致性**：完整的外键约束和事务管理
- 📊 **实时交互**：流畅的用户体验和即时反馈

### 目标用户

| 用户角色 | 主要功能 | 账号示例 |
|---------|---------|---------|
| 学生 | 登录、查看课程、选课、查看成绩 | S001 / 123456 |
| 教师 | 登录、管理课程、考勤录入、成绩录入 | T001 / 123456 |
| 管理员 | 登录、系统管理（学生/教师/课程/实验室） | admin / 123456 |

---

## 技术栈

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 核心编程语言 |
| Spring Boot | 3.2.0 | 应用框架 |
| Spring Data JPA | 3.2.0 | ORM框架 |
| Hibernate | 6.3.1 | JPA实现 |
| MySQL | 8.0 | 关系数据库 |
| HikariCP | - | 数据库连接池 |
| Lombok | 1.18.30 | 代码生成工具 |
| Maven | 3.9 | 项目构建工具 |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4 | 渐进式JavaScript框架 |
| Vue Router | 4.2.5 | 路由管理器 |
| Vite | 5.0.8 | 构建工具 |
| Axios | 1.6.2 | HTTP客户端 |
| Element Plus | 2.4.4 | UI组件库 |
| JavaScript | ES6+ | 编程语言 |

### 开发工具

- **IDE**：IntelliJ IDEA / VS Code
- **数据库工具**：MySQL Workbench / Navicat
- **API测试**：Postman / curl
- **版本控制**：Git

---

## 项目结构

```
d:\789\
├── backend/                    # 后端项目（Spring Boot）
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── labcourse/
│   │       │           ├── config/           # 配置类
│   │       │           │   └── CorsConfig.java
│   │       │           ├── controller/       # 控制器层（8个）
│   │       │           │   ├── AdminController.java
│   │       │           │   ├── AttendanceController.java
│   │       │           │   ├── CourseController.java
│   │       │           │   ├── LabController.java
│   │       │           │   ├── ScoreController.java
│   │       │           │   ├── SelectionController.java
│   │       │           │   ├── StudentController.java
│   │       │           │   └── TeacherController.java
│   │       │           ├── entity/           # 实体类（8个）
│   │       │           │   ├── Admin.java
│   │       │           │   ├── Attendance.java
│   │       │           │   ├── Course.java
│   │       │           │   ├── Lab.java
│   │       │           │   ├── Score.java
│   │       │           │   ├── Selection.java
│   │       │           │   ├── Student.java
│   │       │           │   └── Teacher.java
│   │       │           ├── repository/       # 数据访问层（8个）
│   │       │           │   ├── AdminRepository.java
│   │       │           │   ├── AttendanceRepository.java
│   │       │           │   ├── CourseRepository.java
│   │       │           │   ├── LabRepository.java
│   │       │           │   ├── ScoreRepository.java
│   │       │           │   ├── SelectionRepository.java
│   │       │           │   ├── StudentRepository.java
│   │       │           │   └── TeacherRepository.java
│   │       │           ├── service/          # 服务接口和实现
│   │       │           │   ├── impl/
│   │       │           │   │   ├── AdminServiceImpl.java
│   │       │           │   │   ├── AttendanceServiceImpl.java
│   │       │           │   │   ├── CourseServiceImpl.java
│   │       │           │   │   ├── LabServiceImpl.java
│   │       │           │   │   ├── ScoreServiceImpl.java
│   │       │           │   │   ├── SelectionServiceImpl.java
│   │       │           │   │   ├── StudentServiceImpl.java
│   │       │           │   │   └── TeacherServiceImpl.java
│   │       │           │   ├── AdminService.java
│   │       │           │   ├── AttendanceService.java
│   │       │           │   ├── CourseService.java
│   │       │           │   ├── LabService.java
│   │       │           │   ├── ScoreService.java
│   │       │           │   ├── SelectionService.java
│   │       │           │   ├── StudentService.java
│   │       │           │   └── TeacherService.java
│   │       │           └── LabCourseApplication.java  # 启动类
│   │       └── resources/
│   │           └── application.yml            # 配置文件
│   ├── target/                                # 编译输出目录
│   └── pom.xml                                # Maven配置
│
├── frontend/                   # 前端项目（Vue 3）
│   ├── src/
│   │   ├── api/                # API接口封装（8个）
│   │   │   ├── admin.js
│   │   │   ├── attendance.js
│   │   │   ├── course.js
│   │   │   ├── lab.js
│   │   │   ├── score.js
│   │   │   ├── selection.js
│   │   │   ├── student.js
│   │   │   └── teacher.js
│   │   ├── router/
│   │   │   └── index.js        # 路由配置
│   │   ├── utils/
│   │   │   └── request.js       # HTTP请求工具
│   │   ├── views/               # 页面组件
│   │   │   ├── admin/          # 管理员视图（5个）
│   │   │   │   ├── AdminCourse.vue
│   │   │   │   ├── AdminLab.vue
│   │   │   │   ├── AdminLayout.vue
│   │   │   │   ├── AdminStudent.vue
│   │   │   │   └── AdminTeacher.vue
│   │   │   ├── student/         # 学生视图（3个）
│   │   │   │   ├── StudentCourse.vue
│   │   │   │   ├── StudentLayout.vue
│   │   │   │   └── StudentMyCourse.vue
│   │   │   ├── teacher/         # 教师视图（6个）
│   │   │   │   ├── TeacherAttendance.vue
│   │   │   │   ├── TeacherCourse.vue
│   │   │   │   ├── TeacherLayout.vue
│   │   │   │   ├── TeacherScore.vue
│   │   │   │   └── TeacherStudentList.vue
│   │   │   └── Login.vue        # 登录页面
│   │   ├── App.vue              # 根组件
│   │   └── main.js              # 入口文件
│   ├── index.html
│   ├── package.json             # npm配置
│   └── vite.config.js           # Vite配置
│
├── database/                    # 数据库脚本
│   ├── init_database.sql        # 数据库初始化脚本
│   └── queries.sql              # 查询示例脚本
│
├── test-report.md              # 测试报告
├── test-execution-log.md       # 测试执行日志
└── README.md                   # 项目说明文档
```

---

## 功能模块

### 1. 登录模块

#### 功能描述
- 支持学生、教师、管理员三种角色登录
- 基于学号/工号/用户名和密码的身份验证
- 登录信息本地存储（localStorage）
- 自动跳转至对应角色首页

#### 登录接口

```
POST /api/student/login
POST /api/teacher/login
POST /api/admin/login
```

#### 请求示例

```json
// 学生登录
{
    "studentNo": "S001",
    "password": "123456"
}

// 教师登录
{
    "teacherNo": "T001",
    "password": "123456"
}

// 管理员登录
{
    "username": "admin",
    "password": "123456"
}
```

### 2. 学生模块

#### 功能列表
- **查看课程列表**：浏览所有可选课程及其详细信息
- **选课操作**：选择课程添加到个人选课列表
- **查看已选课程**：查看当前学期的选课情况
- **退课操作**：取消已选课程

#### 核心功能

##### 2.1 课程查询

```
GET /api/course/list
```

返回字段：
- `id`：课程ID
- `course_name`：课程名称
- `teacher_name`：授课教师
- `lab_name`：实验室名称
- `location`：实验室地点
- `course_time`：上课时间
- `max_count`：最大选课人数
- `selected_count`：已选人数

##### 2.2 选课操作

```
POST /api/selection/add
{
    "studentId": 1,
    "courseId": 1
}
```

##### 2.3 查看已选课程

```
GET /api/selection/my/{studentId}
```

### 3. 教师模块

#### 功能列表
- **查看授课课程**：查看自己负责的课程
- **查看选课学生**：查看选修某门课程的学生列表
- **考勤录入**：记录学生的出勤情况
- **成绩录入**：录入学生的课程成绩

#### 核心功能

##### 3.1 考勤录入

```
POST /api/attendance/add
{
    "studentId": 1,
    "courseId": 1,
    "status": "出勤"  // 出勤/请假/缺勤/迟到
}
```

##### 3.2 成绩录入

```
POST /api/score/add
{
    "studentId": 1,
    "courseId": 1,
    "score": 85.5
}
```

##### 3.3 查看选课学生

```
GET /api/selection/studentList/{courseId}
```

### 4. 管理员模块

#### 功能列表
- **学生管理**：增删改查学生账号
- **教师管理**：增删改查教师账号
- **课程管理**：增删改查课程信息
- **实验室管理**：增删改查实验室信息

#### 核心功能

##### 4.1 CRUD操作

```
GET    /api/student/list       # 查询所有学生
POST   /api/student/save       # 新增学生
PUT    /api/student/update     # 更新学生
DELETE /api/student/{id}       # 删除学生
```

##### 4.2 课程管理

```
GET    /api/course/list        # 查询所有课程
POST   /api/course/save        # 新增课程
PUT    /api/course/update      # 更新课程
DELETE /api/course/{id}        # 删除课程
```

---

## 数据库设计

### ER图概述

```
┌─────────┐       ┌─────────┐       ┌─────────┐
│ Student │──────<│Selection│>──────│ Course  │
└─────────┘       └─────────┘       └─────────┘
      │                                  │
      │              ┌─────────┐          │
      └────────────<│  Score  │>──────────┤
      │              └─────────┘          │
      │                                  │
      │              ┌─────────┐          │
      └────────────<│Attendance│>──────────┘
                     └─────────┘

┌─────────┐       ┌─────────┐
│ Course  │──────<│  Lab    │
└─────────┘       └─────────┘
      │
┌─────────┐
│ Teacher │
└─────────┘
      │
┌─────────┐
│  Admin  │
└─────────┘
```

### 数据表结构

#### 1. 学生表 (student)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 学生ID |
| student_no | VARCHAR(20) | NOT NULL, UNIQUE | 学号 |
| name | VARCHAR(50) | NOT NULL | 姓名 |
| gender | VARCHAR(10) | - | 性别 |
| major | VARCHAR(100) | - | 专业 |
| password | VARCHAR(100) | NOT NULL | 密码 |
| created_at | TIMESTAMP | DEFAULT | 创建时间 |
| updated_at | TIMESTAMP | ON UPDATE | 更新时间 |

#### 2. 教师表 (teacher)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 教师ID |
| teacher_no | VARCHAR(20) | NOT NULL, UNIQUE | 工号 |
| name | VARCHAR(50) | NOT NULL | 姓名 |
| title | VARCHAR(50) | - | 职称 |
| password | VARCHAR(100) | NOT NULL | 密码 |
| created_at | TIMESTAMP | DEFAULT | 创建时间 |
| updated_at | TIMESTAMP | ON UPDATE | 更新时间 |

#### 3. 管理员表 (admin)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 管理员ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(100) | NOT NULL | 密码 |
| created_at | TIMESTAMP | DEFAULT | 创建时间 |
| updated_at | TIMESTAMP | ON UPDATE | 更新时间 |

#### 4. 实验室表 (lab)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 实验室ID |
| lab_name | VARCHAR(100) | NOT NULL | 实验室名称 |
| location | VARCHAR(200) | - | 地点 |
| capacity | INT | - | 容量 |

#### 5. 课程表 (course)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 课程ID |
| course_name | VARCHAR(100) | NOT NULL | 课程名称 |
| teacher_id | BIGINT | NOT NULL, FK | 教师ID |
| lab_id | BIGINT | FK | 实验室ID |
| course_time | VARCHAR(100) | - | 上课时间 |
| max_count | INT | DEFAULT 30 | 最大选课人数 |

**外键关系**：
- `teacher_id` → `teacher(id)`
- `lab_id` → `lab(id)`

#### 6. 选课表 (selection)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 选课ID |
| student_id | BIGINT | NOT NULL, FK | 学生ID |
| course_id | BIGINT | NOT NULL, FK | 课程ID |
| select_time | DATETIME | DEFAULT | 选课时间 |

**外键关系**：
- `student_id` → `student(id)`
- `course_id` → `course(id)`

**唯一约束**：
- `UNIQUE(student_id, course_id)` - 防止重复选课

#### 7. 成绩表 (score)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 成绩ID |
| student_id | BIGINT | NOT NULL, FK | 学生ID |
| course_id | BIGINT | NOT NULL, FK | 课程ID |
| score | DECIMAL(5,2) | - | 成绩 |

**外键关系**：
- `student_id` → `student(id)`
- `course_id` → `course(id)`

**唯一约束**：
- `UNIQUE(student_id, course_id)` - 每门课程只有一个成绩

#### 8. 考勤表 (attendance)

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | 考勤ID |
| student_id | BIGINT | NOT NULL, FK | 学生ID |
| course_id | BIGINT | NOT NULL, FK | 课程ID |
| attendance_status | VARCHAR(20) | - | 出勤状态 |

**外键关系**：
- `student_id` → `student(id)`
- `course_id` → `course(id)`

### 索引设计

```sql
-- 课程表索引
CREATE INDEX idx_course_teacher ON course(teacher_id);
CREATE INDEX idx_course_lab ON course(lab_id);

-- 选课表索引
CREATE INDEX idx_selection_student ON selection(student_id);
CREATE INDEX idx_selection_course ON selection(course_id);

-- 成绩表索引
CREATE INDEX idx_score_student ON score(student_id);
CREATE INDEX idx_score_course ON score(course_id);

-- 考勤表索引
CREATE INDEX idx_attendance_student ON attendance(student_id);
CREATE INDEX idx_attendance_course ON attendance(course_id);
```

---

## 快速开始

### 环境要求

| 环境 | 版本要求 |
|------|---------|
| JDK | 17 或更高 |
| Node.js | 18 或更高 |
| MySQL | 8.0 或更高 |
| Maven | 3.6 或更高 |
| npm | 9.x 或更高 |

### 步骤1：数据库配置

#### 1.1 安装MySQL

下载并安装 MySQL 8.0：https://dev.mysql.com/downloads/mysql/

#### 1.2 创建数据库

```bash
# 登录MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE lab_course_system
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

# 退出
EXIT;
```

#### 1.3 初始化数据

```bash
mysql -u root -p lab_course_system < d:\789\database\init_database.sql
```

或者在MySQL命令行中执行：

```bash
mysql> source d:/789/database/init_database.sql
```

### 步骤2：后端配置

#### 2.1 修改数据库配置

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lab_course_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root          # 修改为你的MySQL用户名
    password: 123456         # 修改为你的MySQL密码
```

### 步骤3：前端配置

前端配置已默认为开发环境，无需修改。

---

## 项目启动

### 方式一：命令行启动

#### 1. 启动后端服务

```bash
# 进入后端目录
cd d:\789\backend

# 使用Maven启动（首次启动会自动下载依赖）
mvn spring-boot:run

# 或者先编译再运行
mvn clean compile
mvn spring-boot:run
```

**后端服务启动成功后的输出**：

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

Started LabCourseApplication in 3.766 seconds
```

#### 2. 启动前端服务

```bash
# 新开一个命令行窗口
cd d:\789\frontend

# 安装依赖（仅首次需要）
npm install

# 启动开发服务器
npm run dev
```

**前端服务启动成功后的输出**：

```
  VITE v5.4.11  ready in 1.2 s

  ➜  Local:   http://localhost:3000/
  ➜  Network: http://192.168.1.x:3000/
```

### 方式二：使用IDE启动

#### 后端（IntelliJ IDEA）

1. 打开 IntelliJ IDEA
2. File → Open → 选择 `d:\789\backend`
3. 等待 Maven 导入依赖
4. 右键 `LabCourseApplication.java` → Run
5. 控制台显示 "Started LabCourseApplication" 表示启动成功

#### 前端（VS Code）

1. 打开 VS Code
2. File → Open Folder → 选择 `d:\789\frontend`
3. 打开终端（Terminal → New Terminal）
4. 执行 `npm install` 安装依赖
5. 执行 `npm run dev` 启动开发服务器

### 访问系统

打开浏览器，访问：http://localhost:3000

**默认登录账号**：

| 角色 | 账号 | 密码 |
|------|------|------|
| 学生 | S001 | 123456 |
| 教师 | T001 | 123456 |
| 管理员 | admin | 123456 |

---

## API接口文档

### 基础信息

- **Base URL**：`http://localhost:8080/api`
- **Content-Type**：`application/json`
- **字符编码**：`UTF-8`

### 通用响应格式

```json
// 成功响应
{
    "success": true,
    "data": {...},
    "message": "操作成功"
}

// 失败响应
{
    "success": false,
    "data": null,
    "message": "操作失败：原因"
}
```

### 接口列表

#### 学生接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /student/login | 学生登录 |
| GET | /student/list | 查询所有学生 |
| POST | /student/save | 新增学生 |
| PUT | /student/update | 更新学生 |
| DELETE | /student/{id} | 删除学生 |

#### 教师接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /teacher/login | 教师登录 |
| GET | /teacher/list | 查询所有教师 |
| POST | /teacher/save | 新增教师 |
| PUT | /teacher/update | 更新教师 |
| DELETE | /teacher/{id} | 删除教师 |

#### 管理员接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /admin/login | 管理员登录 |
| GET | /admin/list | 查询所有管理员 |

#### 课程接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /course/list | 查询所有课程 |
| POST | /course/save | 新增课程 |
| PUT | /course/update | 更新课程 |
| DELETE | /course/{id} | 删除课程 |

#### 实验室接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /lab/list | 查询所有实验室 |
| POST | /lab/save | 新增实验室 |
| PUT | /lab/update | 更新实验室 |
| DELETE | /lab/{id} | 删除实验室 |

#### 选课接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /selection/add | 添加选课 |
| DELETE | /selection/delete/{id} | 删除选课 |
| GET | /selection/my/{studentId} | 查询学生的已选课程 |
| GET | /selection/studentList/{courseId} | 查询课程的选课学生 |

#### 成绩接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /score/add | 录入成绩 |
| GET | /score/list | 查询所有成绩 |

#### 考勤接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /attendance/add | 录入考勤 |
| GET | /attendance/list | 查询所有考勤 |

### 接口详细说明

#### 学生登录

```
POST /api/student/login
```

**请求体**：

```json
{
    "studentNo": "S001",
    "password": "123456"
}
```

**成功响应**：

```json
{
    "success": true,
    "data": {
        "id": 1,
        "studentNo": "S001",
        "name": "王小明",
        "gender": "男",
        "major": "计算机科学与技术",
        "password": "123456",
        "createdAt": "2026-05-30T16:30:16",
        "updatedAt": "2026-05-30T16:30:16"
    },
    "message": "登录成功"
}
```

#### 添加选课

```
POST /api/selection/add
```

**请求体**：

```json
{
    "studentId": 1,
    "courseId": 1
}
```

**成功响应**：

```json
{
    "success": true,
    "message": "选课成功"
}
```

**失败响应**（重复选课或人数已满）：

```json
{
    "success": false,
    "message": "选课失败（已选或人数已满）"
}
```

#### 录入考勤

```
POST /api/attendance/add
```

**请求体**：

```json
{
    "studentId": 1,
    "courseId": 1,
    "status": "出勤"
}
```

**status可选值**：`出勤`、`请假`、`缺勤`、`迟到`

#### 录入成绩

```
POST /api/score/add
```

**请求体**：

```json
{
    "studentId": 1,
    "courseId": 1,
    "score": 85.5
}
```

---

## 测试报告

### 测试概述

**测试时间**：2026年5月30日  
**测试类型**：端到端（E2E）自动化测试  
**测试范围**：学生选课、教师考勤录入、成绩管理三大核心模块  
**测试结果**：🎉 **全部通过**

### 测试用例统计

| 功能模块 | 测试用例数 | 通过数 | 失败数 | 通过率 |
|----------|-----------|--------|--------|--------|
| 学生登录 | 1 | 1 | 0 | 100% |
| 课程查询 | 1 | 1 | 0 | 100% |
| 学生选课 | 2 | 2 | 0 | 100% |
| 选课查询 | 1 | 1 | 0 | 100% |
| 教师登录 | 1 | 1 | 0 | 100% |
| 考勤录入 | 1 | 1 | 0 | 100% |
| 成绩录入 | 2 | 2 | 0 | 100% |
| **总计** | **9** | **9** | **0** | **100%** |

### 性能指标

| 指标 | 目标 | 实际 | 状态 |
|------|------|------|------|
| API响应时间 | < 1000ms | < 500ms | ✅ 优于目标 |
| 数据插入成功率 | 100% | 100% | ✅ 达标 |
| 数据查询准确性 | 100% | 100% | ✅ 达标 |
| 清理操作成功率 | 100% | 100% | ✅ 达标 |

### 测试报告文件

- [test-report.md](test-report.md) - 详细测试报告
- [test-execution-log.md](test-execution-log.md) - 测试执行日志

---

## 开发指南

### 项目架构

本项目采用经典的三层架构设计：

```
┌─────────────────────────────────────────┐
│         表示层 (Controller)              │
│   处理HTTP请求、参数验证、响应封装        │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│          业务逻辑层 (Service)            │
│   处理业务逻辑、事务管理、数据校验        │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│         数据访问层 (Repository)           │
│   数据持久化、数据库操作、ORM映射        │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│              数据库 (MySQL)              │
│          数据存储与查询                  │
└─────────────────────────────────────────┘
```

### 后端开发规范

#### 1. 实体类定义

```java
@Entity
@Table(name = "student")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "student_no", unique = true, nullable = false)
    private String studentNo;
    
    // Getter和Setter方法
}
```

#### 2. Repository定义

```java
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentNoAndPassword(String studentNo, String password);
}
```

#### 3. Service定义

```java
public interface StudentService {
    Student login(String studentNo, String password);
    List<Student> list();
    boolean save(Student student);
}

@Service
public class StudentServiceImpl implements StudentService {
    @Autowired
    private StudentRepository studentRepository;
    
    @Override
    public Student login(String studentNo, String password) {
        return studentRepository.findByStudentNoAndPassword(studentNo, password)
                               .orElse(null);
    }
}
```

#### 4. Controller定义

```java
@RestController
@RequestMapping("/student")
public class StudentController {
    @Autowired
    private StudentService studentService;
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> data) {
        Student student = studentService.login(data.get("studentNo"), data.get("password"));
        Map<String, Object> result = new HashMap<>();
        if (student != null) {
            result.put("success", true);
            result.put("data", student);
            result.put("message", "登录成功");
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }
        return ResponseEntity.ok(result);
    }
}
```

### 前端开发规范

#### 1. API封装

```javascript
// src/api/student.js
import request from '../utils/request'

export function login(data) {
  return request({
    url: '/student/login',
    method: 'post',
    data
  })
}

export function getList() {
  return request({
    url: '/student/list',
    method: 'get'
  })
}
```

#### 2. 请求工具

```javascript
// src/utils/request.js
import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  return config
})

request.interceptors.response.use(
  response => response.data,
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

export default request
```

#### 3. 组件示例

```vue
<template>
  <el-button @click="handleLogin">登录</el-button>
</template>

<script setup>
import { login } from '@/api/student'

const handleLogin = async () => {
  try {
    const result = await login({ studentNo: 'S001', password: '123456' })
    if (result.success) {
      ElMessage.success('登录成功')
      localStorage.setItem('user', JSON.stringify(result.data))
    }
  } catch (error) {
    ElMessage.error('登录失败')
  }
}
</script>
```

### 添加新功能

#### 1. 后端添加

1. **创建实体类**：在 `entity` 包下创建新实体
2. **创建Repository**：在 `repository` 包下创建数据访问接口
3. **创建Service**：在 `service` 包下创建服务接口和实现
4. **创建Controller**：在 `controller` 包下创建控制器

#### 2. 前端添加

1. **创建API**：在 `api` 目录下创建新的API文件
2. **创建视图**：在 `views` 目录下创建新的Vue组件
3. **配置路由**：在 `router/index.js` 中添加路由配置

### 代码质量检查

```bash
# 后端编译检查
cd backend
mvn clean compile

# 前端构建检查
cd frontend
npm run build
```

---

## 常见问题

### Q1: 端口被占用

**问题**：启动时报错 "Port 8080 was already in use"

**解决方案**：

```bash
# Windows查看占用端口的进程
netstat -ano | findstr :8080

# 结束占用进程
taskkill /PID <进程ID> /F
```

### Q2: 数据库连接失败

**问题**：Spring Boot启动失败，报错 "Connection refused"

**解决方案**：

1. 检查MySQL服务是否启动：
   ```bash
   net start mysql
   ```

2. 检查数据库配置是否正确
3. 检查数据库是否存在：
   ```bash
   mysql -u root -p -e "SHOW DATABASES;"
   ```

### Q3: 前端无法访问后端API

**问题**：前端页面提示网络错误

**解决方案**：

1. 确保后端服务已启动
2. 检查代理配置（vite.config.js）
3. 检查CORS配置（后端CorsConfig.java）

### Q4: Lombok不生效

**问题**：实体类缺少getter/setter方法

**解决方案**：

1. 确保pom.xml中Lombok版本正确
2. IDE中安装Lombok插件：
   - IntelliJ IDEA: Settings → Plugins → 搜索 "Lombok"
3. 启用注解处理器：
   - IntelliJ IDEA: Settings → Build → Compiler → Annotation Processors → 勾选 "Enable annotation processing"

### Q5: 数据库表结构与实体类不匹配

**问题**：JPA验证失败

**解决方案**：

1. 检查数据库表结构是否正确创建
2. 确保application.yml中 `spring.jpa.hibernate.ddl-auto` 设置为 `validate`
3. 可以临时改为 `update` 自动更新表结构（生产环境不推荐）

### Q6: Maven依赖下载慢

**问题**：Maven编译时下载依赖很慢

**解决方案**：

配置阿里云镜像（~/.m2/settings.xml）：

```xml
<mirrors>
  <mirror>
    <id>aliyunmaven</id>
    <mirrorOf>*</mirrorOf>
    <name>阿里云公共仓库</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

---

## 文档目录

项目包含以下文档文件：

- **README.md** - 项目说明文档（本文档）
- **test-report.md** - 详细测试报告
- **test-execution-log.md** - 测试执行日志
- **实验选课系统_spring_boot_vue_课程设计实施步骤.md** - 实施步骤
- **课程设计报告.md** - 课程设计报告

---

## 版本信息

- **项目版本**：1.0.0
- **发布日期**：2026年5月30日
- **Spring Boot**：3.2.0
- **Vue**：3.4
- **MySQL**：8.0

---

## 贡献者

本项目为课程设计项目，由学生独立开发完成。

---

## 许可证

本项目仅用于学习和教育目的。

---

<p align="center">
  <strong>感谢使用实验选课系统！</strong>
</p>

**文档版本**：V1.0  
**最后更新**：2026年5月30日
