# 模块目录结构与集成指南

## 标准目录结构

每个业务模块位于 `specflow-api/src/main/java/com/specflow/api/modules/{module}/`，遵循 DDD Light 四层架构：

```
{module}/
├── interfaces/                          # 接口层：Controller + DTOs
│   ├── {Entity}Controller.java
│   └── dto/
│       ├── {Action}{Entity}Request.java  # 如 CreatePetRequest, UpdateUserRequest
│       └── {Entity}Response.java         # 如 PetResponse, UserResponse
├── application/                         # 应用层：Service（用例编排）
│   └── {Entity}Service.java
├── domain/                              # 领域层：纯 POJO，无框架注解
│   ├── entity/
│   │   └── {Entity}.java
│   ├── repository/
│   │   └── {Entity}Repository.java      # 接口定义
│   └── service/                         # 领域服务（可选）
│       └── {DomainService}.java
└── infrastructure/                      # 基础设施层：框架集成
    └── persistence/
        ├── {Entity}DO.java              # 数据对象（MyBatis-Plus 注解）
        ├── {Entity}Mapper.java          # MyBatis Mapper 接口
        ├── {Entity}RepositoryImpl.java  # Repository 实现
        └── converter/
            └── {Entity}Converter.java   # DO ↔ Entity 双向转换
```

## 数据库文件位置

| 文件类型 | 路径 |
|---------|------|
| Flyway 迁移 | `specflow-api/src/main/resources/db/migration/V{x.x}__{desc}.sql` |
| H2 测试 Schema | `specflow-api/src/test/resources/schema-h2.sql`（追加到末尾） |

## 测试文件位置

| 测试类型 | 路径 |
|---------|------|
| Service 单元测试 | `specflow-api/src/test/java/com/specflow/api/modules/{module}/application/{Entity}ServiceTest.java` |
| Controller 集成测试 | `specflow-api/src/test/java/com/specflow/api/modules/{module}/interfaces/{Entity}ControllerTest.java` |

## 命名规范

### 实体与文件命名

| 类别 | 规则 | 正确示例 | 错误示例 |
|------|------|---------|---------|
| 模块目录 | 小写字母，多词直接连接 | `petcare` | `pet-care`, `PetCare` |
| 实体类 | 大驼峰单数 | `CareRecord` | `CareRecords`, `careRecord` |
| DO 类 | `{Entity}DO` | `CareRecordDO` | `CareRecordDataObject` |
| Mapper | `{Entity}Mapper` | `CareRecordMapper` | `CareRecordDao` |
| Repository 接口 | `{Entity}Repository` | `CareRecordRepository` | `ICareRecordRepository` |
| Repository 实现 | `{Entity}RepositoryImpl` | `CareRecordRepositoryImpl` | `CareRecordRepositoryImplV2` |
| Converter | `{Entity}Converter` | `CareRecordConverter` | `CareRecordMapping` |
| Service | `{Entity}Service` | `CareRecordService` | `CareRecordApplicationService` |
| Controller | `{Entity}Controller` | `CareRecordController` | `CareRecordApi` |

### 数据库命名

| 类别 | 规则 | 正确示例 |
|------|------|---------|
| 表名 | 蛇形复数 | `care_records`, `users`, `pets` |
| 列名 | 蛇形 | `owner_id`, `created_at`, `deleted_at` |
| 索引 | `idx_{table}_{column}` | `idx_care_records_owner_id` |
| 唯一索引 | `idx_{table}_{column}` | `idx_users_email`（配合 WHERE）|
| 外键 | `fk_{child}_{parent}` | `fk_pets_users` |

### API 路径

| 规则 | 正确示例 |
|------|---------|
| 使用复数蛇形（或 kebab-case） | `/api/v1/care-records` |
| 资源嵌套最多两层 | `/api/v1/users/{userId}/pets` |
| 版本固定 v1 | `/api/v1/...` |

### DTO 命名

| 操作 | Request 类名 | Response 类名 |
|------|-------------|---------------|
| 创建 | `Create{Entity}Request` 或 `Add{Entity}Request` | `{Entity}Response` |
| 更新 | `Update{Entity}Request` | `{Entity}Response` |
| 登录 | `LoginRequest` | `LoginResponse` |
| 注册 | `RegisterRequest` | `UserResponse` |
| 查询 | 无 Request 或 `Query{Entity}Request` | `{Entity}Response` |

## 集成点

### 1. AuthInterceptor — 公开路径

**文件**: `specflow-api/src/main/java/com/specflow/api/config/AuthInterceptor.java`

如果新模块有不需要登录的接口（如注册、登录），需要将路径添加到 `EXCLUDE_PATHS`：

```java
private static final List<String> EXCLUDE_PATHS = List.of(
        "/api/v1/users/register",
        "/api/v1/users/login",
        "/api/v1/sessions",
        // 新增公开路径...
        "/actuator",
        "/swagger-ui",
        "/api-docs",
        "/v3/api-docs",
        "/error"
);
```

路径匹配逻辑为**前缀匹配**。

### 2. Flyway 版本号

查看现有迁移文件最大版本号，新模块版本号递增：

```
V1.0__create_session_table.sql      → auth 模块
V1.1__create_user_and_pet_tables.sql → user 模块
V1.2__create_{module}_tables.sql    → 新模块（版本号 +0.1）
```

### 3. H2 测试 Schema

在 `specflow-api/src/test/resources/schema-h2.sql` 末尾追加新表定义。注意 H2 与 PostgreSQL 的差异：

| PostgreSQL | H2 替代方案 |
|-----------|------------|
| `CREATE TRIGGER ... EXECUTE FUNCTION` | 不写（H2 不需要 updated_at 触发器） |
| `COMMENT ON TABLE/COLUMN` | 不写 |
| `CREATE UNIQUE INDEX ... WHERE deleted = FALSE` | `CREATE UNIQUE INDEX ...`（去掉 WHERE 子句） |
| `LOWER(column)` 在索引中 | 直接用 `column`（去掉函数索引） |
| `IF NOT EXISTS` | 始终加上 |

### 4. MyBatis-Plus 全局配置

已在 `application.yml` 中配置，新模块自动继承：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      id-type: assign_uuid
  configuration:
    map-underscore-to-camel-case: true
```

### 5. 共享组件（specflow-common）

新模块可直接使用：

| 组件 | 用法 |
|------|------|
| `Result<T>` | Controller 返回值：`Result.success(data)`, `Result.failure(code, msg)` |
| `BusinessException` | 业务异常：`throw new BusinessException("CODE", "message")` |
| `NotFoundException` | 资源不存在：`throw new NotFoundException("xxx不存在")` |
| `AuthenticationException` | 认证失败：`throw new AuthenticationException("message")` |

这些异常会被 `GlobalExceptionHandler` 自动捕获并转换为对应 HTTP 状态码的 `Result<T>` 响应。
