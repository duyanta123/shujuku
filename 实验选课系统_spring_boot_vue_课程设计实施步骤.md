# 实验选课系统（MySQL + SpringBoot + Vue）课程设计实施步骤

## 一、项目目标

本系统为《数据库原理》课程设计中的“实验选课系统”。

采用：

- 数据库：MySQL
- 后端：SpringBoot
- 前端：Vue
- 开发工具：IDEA + Navicat + VSCode

系统仅实现老师要求的核心功能，不扩展复杂业务。

---

# 二、系统功能（核心版）

系统分为三类用户：

## 1. 学生功能

- 登录
- 查看实验课程
- 选课
- 退课
- 查看自己的课程

---

## 2. 教师功能

- 登录
- 查看自己教授的课程
- 查看选课学生名单
- 录入成绩
- 录入考勤

---

## 3. 管理员功能

- 登录
- 学生管理
- 教师管理
- 实验课程管理
- 实验室管理

---

# 三、开发步骤总览

建议按照下面顺序开发：

```text
第一阶段：数据库设计
第二阶段：SpringBoot 后端开发
第三阶段：Vue 前端开发
第四阶段：前后端联调
第五阶段：系统测试
第六阶段：课程设计报告
```

不要一开始就做前端界面。

数据库和接口优先。

---

# 四、第一阶段：数据库设计

这一阶段最重要。

先把数据库做好，再开发系统。

---

# 五、数据库表设计

建议只做下面 8 张表。

已经完全够课程设计。

---

## 1. 学生表（student）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| student_no | varchar | 学号 |
| name | varchar | 姓名 |
| gender | varchar | 性别 |
| major | varchar | 专业 |
| password | varchar | 密码 |

---

## 2. 教师表（teacher）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| teacher_no | varchar | 工号 |
| name | varchar | 姓名 |
| title | varchar | 职称 |
| password | varchar | 密码 |

---

## 3. 管理员表（admin）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| username | varchar | 用户名 |
| password | varchar | 密码 |

---

## 4. 实验室表（lab）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| lab_name | varchar | 实验室名称 |
| location | varchar | 地点 |
| capacity | int | 容量 |

---

## 5. 课程表（course）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| course_name | varchar | 课程名 |
| teacher_id | bigint | 教师ID |
| lab_id | bigint | 实验室ID |
| course_time | varchar | 上课时间 |
| max_count | int | 最大人数 |

---

## 6. 选课表（selection）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| student_id | bigint | 学生ID |
| course_id | bigint | 课程ID |
| select_time | datetime | 选课时间 |

---

## 7. 成绩表（score）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| student_id | bigint | 学生ID |
| course_id | bigint | 课程ID |
| score | decimal | 成绩 |

---

## 8. 考勤表（attendance）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| student_id | bigint | 学生ID |
| course_id | bigint | 课程ID |
| attendance_status | varchar | 出勤状态 |

---

# 六、E-R 图关系

建议关系如下：

```text
学生 —— 选课 —— 课程
教师 —— 课程
实验室 —— 课程
学生 —— 成绩 —— 课程
学生 —— 考勤 —— 课程
```

课程设计报告中一定要画 E-R 图。

这是重点得分项。

---

# 七、第二阶段：SpringBoot 后端开发

建议版本：

- JDK 17
- SpringBoot 3
- MySQL 8
- Maven

---

# 八、创建 SpringBoot 项目

## 需要的依赖

```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
mysql-connector-j
lombok
spring-boot-devtools
```

如果你熟悉 MyBatis，也可以使用：

```xml
mybatis-plus-boot-starter
```

课程设计推荐 MyBatis-Plus。

开发更快。

---

# 九、项目结构

推荐结构：

```text
src/main/java
├── controller
├── service
├── mapper
├── entity
├── dto
├── config
└── utils
```

---

# 十、配置数据库

application.yml：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lab_course_system
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
```

---

# 十一、实体类开发

例如 Student：

```java
@Data
@TableName("student")
public class Student {

    private Long id;

    private String studentNo;

    private String name;

    private String gender;

    private String major;

    private String password;
}
```

---

# 十二、后端接口设计

只做核心接口。

---

# 十三、管理员接口

## 学生管理

```text
GET /student/list
POST /student/add
PUT /student/update
DELETE /student/delete/{id}
```

---

## 教师管理

```text
GET /teacher/list
POST /teacher/add
PUT /teacher/update
DELETE /teacher/delete/{id}
```

---

## 课程管理

```text
GET /course/list
POST /course/add
PUT /course/update
DELETE /course/delete/{id}
```

---

# 十四、学生接口

## 查询课程

```text
GET /course/list
```

---

## 选课

```text
POST /selection/add
```

---

## 退课

```text
DELETE /selection/delete/{id}
```

---

## 查询已选课程

```text
GET /selection/my
```

---

# 十五、教师接口

## 查看学生名单

```text
GET /selection/studentList/{courseId}
```

---

## 录入成绩

```text
POST /score/add
```

---

## 录入考勤

```text
POST /attendance/add
```

---

# 十六、课程设计推荐 SQL 功能

课程设计里最好体现：

- 主键
- 外键
- 连接查询
- 分组统计
- 存储过程
- 触发器

这样老师会觉得数据库设计完整。

---

# 十七、推荐实现的 SQL 查询

## 1. 查询某学生已选课程

```sql
SELECT c.course_name
FROM selection s
JOIN course c ON s.course_id = c.id
WHERE s.student_id = 1;
```

---

## 2. 查询某课程人数

```sql
SELECT course_id, COUNT(*)
FROM selection
GROUP BY course_id;
```

---

## 3. 查询教师课程

```sql
SELECT *
FROM course
WHERE teacher_id = 1;
```

---

# 十八、推荐实现的触发器

建议只做一个简单触发器。

例如：

```text
选课后自动记录选课时间
```

已经够课程设计使用。

---

# 十九、第三阶段：Vue 前端开发

推荐：

- Vue3
- Axios
- Element Plus

---

# 二十、前端页面

只做下面这些页面。

---

## 登录页面

- 学生登录
- 教师登录
- 管理员登录

---

## 管理员页面

- 学生管理
- 教师管理
- 课程管理
- 实验室管理

---

## 学生页面

- 课程列表
- 我的课程

---

## 教师页面

- 我的课程
- 学生名单
- 成绩录入
- 考勤录入

---

# 二十一、前端目录结构

```text
src
├── api
├── views
├── router
├── components
└── utils
```

---

# 二十二、接口调用

使用 axios：

```javascript
axios.get('/course/list')
```

---

# 二十三、第四阶段：前后端联调

联调顺序：

```text
1. 登录
2. 查询课程
3. 选课
4. 退课
5. 成绩录入
6. 考勤录入
```

不要同时调多个功能。

一个一个测试。

---

# 二十四、第五阶段：系统测试

课程设计报告中一定要写测试。

建议写：

## 功能测试

| 功能 | 输入 | 结果 |
|---|---|---|
| 登录 | 正确账号密码 | 登录成功 |
| 选课 | 正常选课 | 选课成功 |
| 退课 | 已选课程 | 退课成功 |

---

## 异常测试

| 功能 | 输入 | 结果 |
|---|---|---|
| 登录 | 错误密码 | 提示错误 |
| 选课 | 重复选课 | 提示失败 |

---

# 二十五、第六阶段：课程设计报告

建议目录：

```text
第一章 绪论
第二章 需求分析
第三章 数据库设计
第四章 系统设计
第五章 系统实现
第六章 系统测试
总结
参考文献
附录
```

---

# 二十六、第三章数据库设计重点

这一章最重要。

必须包括：

- 数据库需求分析
- E-R 图
- 数据字典
- 表结构设计
- 主键外键
- 关系模式
- SQL建表语句

---

# 二十七、第四章系统设计

建议写：

- 系统架构图
- 功能模块图
- 系统流程图
- 登录流程
- 选课流程

---

# 二十八、第五章系统实现

建议放：

- 核心代码
- 页面截图
- SQL截图
- 登录页面
- 选课页面
- 成绩录入页面

---

# 二十九、最容易丢分的问题

## 1. 不画 E-R 图

严重扣分。

---

## 2. 没有主键外键

老师会认为数据库设计不规范。

---

## 3. 页面很多但数据库很弱

数据库课最重要的是数据库。

不是页面。

---

## 4. 系统太复杂做不完

不要扩展功能。

只做核心功能。

---

# 三十、推荐开发顺序（非常重要）

正确顺序：

```text
数据库
→ SQL
→ 后端接口
→ 前端页面
→ 测试
→ 报告
```

错误顺序：

```text
先做前端页面
```

这样最后很容易烂尾。

---

# 三十一、推荐时间安排

## 第1天

- 数据库设计
- 建表
- E-R 图

---

## 第2天

- SpringBoot 后端
- 接口开发

---

## 第3天

- Vue 页面
- 联调

---

## 第4天

- 测试
- 截图
- 完善功能

---

## 第5天

- 写课程设计报告

---

# 三十二、课程设计最核心的内容

老师真正看重的是：

```text
1. 数据库设计
2. SQL能力
3. 表关系
4. 功能完整性
5. 报告规范性
```

不是页面炫酷程度。

---

# 三十三、建议最终实现效果

建议目标：

```text
能登录
能选课
能退课
能录成绩
能查询
```

已经完全够课程设计。

不要过度开发。

---

# 三十四、最终建议

本项目最重要的是：

- 控制规模
- 先数据库后前端
- 只做核心功能
- 按模块逐步完成

如果数据库部分做规范，这个课程设计已经能达到比较不错的完成度。

