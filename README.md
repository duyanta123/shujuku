# 实验选课系统

基于 Spring Boot、Fastify BFF、Vue 3 和 MySQL 的高校实验课程管理系统，面向学生、教师和管理员三类角色，覆盖实验选课、课程管理、考勤签到、成绩录入、学院专业管理和基础权限控制等业务场景。

## 项目概览

本项目采用前后端分离架构，并在前端和后端之间加入 BFF 中间层：

- `frontend/`：Vue 3 + Vite 前端应用，提供学生端、教师端、管理员端页面。
- `bff/`：Fastify BFF 服务，负责 Cookie Token、请求转发、安全响应头、限流和日志脱敏。
- `backend/`：Spring Boot 后端服务，提供认证、业务接口、权限控制和数据持久化。
- `database/`：MySQL 初始化脚本、查询脚本和数据库设计文档。
- `tests/`、`frontend/tests/`、`backend/src/test/`、`bff/tests/`：自动化测试用例。

## 核心功能

### 学生端

- 查看可选实验课程、课程容量、任课教师、实验室和上课时间。
- 在线选课和退课，并进行时间冲突检测。
- 查看已选课程和周课表。
- 课程签到，支持根据上课时间自动判定出勤、迟到等状态。
- 查看个人签到历史。
- 网络异常时支持离线签到队列，恢复后自动同步。

### 教师端

- 查看本人授课课程。
- 查看课程选课学生名单。
- 管理课程考勤记录，支持将缺勤调整为请假并记录原因。
- 录入和修改学生成绩。
- 导出课程考勤数据。

### 管理员端

- 管理学生、教师、课程、实验室、学院和专业。
- 配置课程类型，包括必修课和选修课。
- 管理专业必修课程关系。
- 维护学院、专业、课程、实验室之间的层级和关联数据。

### 安全与稳定性

- JWT 认证，BFF 模式下使用 HttpOnly Cookie 保存访问令牌和刷新令牌。
- Refresh Token 轮换，刷新接口只接受 `bff_refresh_token` Cookie。
- BCrypt 密码加密，并支持启动时迁移演示数据中的明文密码。
- 登录失败次数持久化，连续失败后自动锁定账号。
- Spring Security 角色权限控制。
- 后端全局异常处理和参数校验。
- BFF 侧启用 Helmet、安全 CORS、限流和敏感日志脱敏。
- 生产环境配置校验，避免使用演示数据库凭据或弱密钥。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 后端 | Java 25、Spring Boot 4、Spring Security、Spring Data JPA、Hibernate、JJWT、Maven |
| BFF | Node.js、Fastify 5、@fastify/cookie、@fastify/cors、@fastify/helmet、jsonwebtoken、pino |
| 前端 | Vue 3、Vue Router、Vite、Element Plus、Axios、ExcelJS |
| 数据库 | MySQL 8.0、InnoDB、utf8mb4 |
| 测试 | JUnit 5、Vitest、Playwright |
| CI | GitHub Actions |

## 目录结构

```text
.
├── .github/                  # GitHub Actions 工作流
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/        # 后端源码
│   ├── src/main/resources/   # 应用配置
│   └── src/test/java/        # 后端测试
├── bff/                      # Fastify BFF 服务
│   ├── src/                  # BFF 源码
│   └── tests/                # BFF 测试
├── database/                 # 数据库脚本与设计文档
├── frontend/                 # Vue 前端
│   ├── src/                  # 前端源码
│   └── tests/e2e/            # Playwright E2E 测试
├── CODE_WIKI.md              # 代码说明文档
├── implementation-guide.md   # 实施指南
└── README.md                 # 项目说明
```

## 环境要求

- JDK 25
- Maven 3.9+
- Node.js 20+
- npm 10+
- MySQL 8.0+

## 数据库初始化

1. 启动 MySQL。
2. 执行初始化脚本：

```bash
mysql -u root -p < database/init_database.sql
```

脚本默认创建数据库 `lab_course_system`，并包含本地演示数据。生产环境不要直接导入演示账号数据。

## 环境变量

### 后端

后端默认读取 `backend/src/main/resources/application.yml`，常用环境变量如下：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/lab_course_system?...` | MySQL 连接地址 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `123456` | 数据库密码 |
| `SERVER_PORT` | `8080` | 后端端口 |
| `JWT_SECRET` | 无默认值 | JWT 密钥，必须配置，建议不少于 32 位 |
| `JWT_EXPIRATION` | `86400000` | Token 过期时间，单位毫秒 |

### BFF

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `BFF_PORT` | `4000` | BFF 服务端口 |
| `BACKEND_URL` | `http://localhost:8080` | 后端服务地址 |
| `BACKEND_TIMEOUT_MS` | `10000` | 转发请求超时时间 |
| `JWT_SECRET` | 无默认值 | 需与后端保持一致 |
| `JWT_EXPIRATION` | `86400000` | Token 过期时间 |
| `RATE_LIMIT_MAX` | 开发环境 `1000`，生产环境 `100` | 限流阈值 |
| `RATE_LIMIT_WINDOW` | `1 minute` | 限流窗口 |

### 前端

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `VITE_BFF_ENABLED` | `true` | 是否通过 BFF 转发 `/api` 请求 |

当前 Vite 开发服务器默认运行在 `3000` 端口。启用 BFF 时，`/api` 会代理到 `http://localhost:4000`；关闭 BFF 时会代理到 `http://localhost:8080`。

## 本地启动

### 1. 启动后端

```bash
cd backend
set JWT_SECRET=your-jwt-secret-at-least-32-bytes
mvn spring-boot:run
```

PowerShell 可使用：

```powershell
cd backend
$env:JWT_SECRET="your-jwt-secret-at-least-32-bytes"
mvn spring-boot:run
```

### 2. 启动 BFF

```bash
cd bff
npm install
set JWT_SECRET=your-jwt-secret-at-least-32-bytes
npm run dev
```

PowerShell 可使用：

```powershell
cd bff
$env:JWT_SECRET="your-jwt-secret-at-least-32-bytes"
npm run dev
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问地址：

- 前端：`http://localhost:3000`
- BFF：`http://localhost:4000`
- 后端：`http://localhost:8080`

## 测试

### 后端测试

```bash
cd backend
mvn test
```

### BFF 测试

```bash
cd bff
npm test
```

### 前端单元测试

```bash
cd frontend
npm test
```

### 前端 E2E 测试

```bash
cd frontend
npm run test:e2e
```

也可以按模块运行：

```bash
npm run test:e2e:auth
npm run test:e2e:college
npm run test:e2e:major
npm run test:e2e:business
npm run test:e2e:ui
```

## 常用账号

初始化脚本包含本地演示账号，具体账号和密码请以 `database/init_database.sql` 中 seed 数据为准。演示账号仅用于本地开发和课程设计展示，生产环境应重新创建账号并配置强密码。

## 相关文档

- [代码说明文档](CODE_WIKI.md)
- [实施指南](implementation-guide.md)
- [数据库设计](database/database-design.md)
- [数据库变更同步说明](database/DB_CHANGE_SYNC_NOTICE.md)
- [前端 UI 规范](frontend/UI_SPECIFICATION.md)
- [前端 UI 重构指南](frontend/UI_REFACTOR_GUIDE.md)

## 部署提示

- 生产环境必须设置强 `JWT_SECRET`，并确保后端与 BFF 使用同一个值。
- 不要使用默认数据库账号、默认密码或演示 seed 数据。
- 后端可使用 `application-prod.yml` 启动生产配置。
- 建议将前端静态资源构建后交由 Nginx 或对象存储托管，API 请求统一转发到 BFF。
- MySQL 需启用 `utf8mb4` 字符集，避免中文数据写入异常。
