# 数据库变更同步说明 — v2.0

## 变更版本
- 版本标签：`v2.0-db-refactor`
- 变更日期：2026-06-20
- 变更分支：`feature/db-refactor`

## 变更概述
本次更新对数据库进行了结构优化，主要包括：
1. 存储过程集成到业务代码
2. 删除过渡字段（college/major VARCHAR列）
3. 删除冗余 course_teacher 表
4. 修复 queries.sql Bug
5. 新增数据库设计文档

## 变更影响
### 数据库结构变更（需刷新本地数据库）
| 变更类型 | 影响表 | 变更内容 |
|---------|-------|---------|
| 删除列 | student | 删除 `college` VARCHAR, `major` VARCHAR |
| 删除列 | teacher | 删除 `college` VARCHAR |
| 删除列 | course | 删除 `college` VARCHAR |
| 删除列 | lab | 删除 `college` VARCHAR |
| 删除索引 | student | 删除旧字段对应的索引 |
| 删除表 | course_teacher | 删除冗余表 |

### 代码变更（需重新编译）
- 实体类字段变更：Student/Teacher/Course/Lab 的 getCollege()/getMajor() 已改为 getCollegeId()/getMajorId()
- 存储过程调用：AttendanceServiceImpl 和 SelectionServiceImpl 改为调用存储过程

## 本地环境同步步骤

### 1. 拉取最新代码
```bash
git checkout main
git pull origin main
```

### 2. 备份现有数据库（重要！）
```bash
mysqldump -u root -p lab_course_system > lab_course_system_backup_$(date +%Y%m%d).sql
```

### 3. 执行数据库更新脚本
```bash
mysql -u root -p < database/init_database.sql
```

### 4. 验证数据完整性
```bash
mysql -u root -p < database/validate_data.sql
```

### 5. 重新编译后端
```bash
cd backend
mvn clean compile
```

### 6. 运行测试验证
```bash
mvn test
```

## 回滚方案
如需回滚，请执行：
```bash
# 恢复数据库
mysql -u root -p lab_course_system < lab_course_system_backup_YYYYMMDD.sql

# 恢复旧版代码
git checkout v2.0-db-refactor~1
```

## 注意事项
- 执行数据库变更前务必先备份
- 确保所有 college_id/major_id 外键关联数据完整有效
- 如有其他开发者本地数据库未同步，联调时可能出现字段不匹配错误