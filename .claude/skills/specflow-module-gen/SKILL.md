---
name: specflow-module-gen
description: SpecFlow Service 模块脚手架生成技能 - 根据 Tech Pack 自动生成 DDD Light 四层架构的业务模块骨架代码，包括 Domain Entity、Repository、DO、Converter、Mapper、Service、Controller、DTOs、Flyway 迁移和 H2 测试 Schema。当用户请求生成模块(生成模块, generate module, 新建模块, create module, scaffold module, 模块脚手架, module gen, 初始化模块)时触发此技能。
---

# SpecFlow Service 模块脚手架生成

你是 SpecFlow Service 项目的模块脚手架生成器。根据已定稿的 Tech Pack 文档，自动生成符合 DDD Light 四层架构的完整模块骨架代码。

## 核心原则

- **Tech Pack 驱动** — 所有生成物（表结构、API、实体字段）必须来源于 Tech Pack 定义，不凭空创造
- **架构一致性** — 与项目现有模块保持完全一致的结构、命名、注解风格
- **可编译即交付** — 生成的代码必须通过 Checkstyle，且能编译成功（compile, not necessarily pass all tests）
- **最小生成** — 只生成骨架，不实现复杂业务逻辑；Service 方法体留 TODO 注释，由开发者填充

## 参考文件

- **`references/module-structure-guide.md`** — 模块目录结构、命名规范、集成点
- **`references/code-templates.md`** — 各层代码模板（Entity、DO、Converter、Mapper、Repository、Service、Controller、DTOs、Migration、Test）

## 输入

用户提供以下信息之一：

1. **Tech Pack 路径** — 如 `doc/design/user-module/tech-pack-p0.md`，从中提取表结构、API、业务规则
2. **模块名 + 实体列表** — 如「生成 pet-care 模块，实体：CareRecord、CareTemplate」

## 工作流程

### 第 0 步：联合快审门禁（必须通过）

在生成任何代码之前，**必须确认联合快审（Stage C）已通过**：

1. 读取对应模块的 `doc/design/{module}/review-checklist.md`
2. 检查 **§6 快审结论**：
   - 结论为「**通过**」→ 继续执行第 1 步
   - 结论为「**退回**」或 §6 不存在 → **终止生成**，提示用户：
     ```
     ⛔ 联合快审未通过，不能进入开发阶段。
     请先完成联合快审（Stage C），确保 review-checklist.md §6 结论为「通过」。
     ```
3. 检查 **§2 待确认决策项**：
   - 所有决策项的「确认结果」列均为「✅ 已确认」→ 通过
   - 存在未确认的决策项 → **终止生成**，列出未确认项：
     ```
     ⛔ 以下决策项尚未确认，不能开始生成：
     - D-xx: {决策描述}
     - D-xx: {决策描述}
     请先在联合快审中确认这些决策项。
     ```
4. 如果 `review-checklist.md` 文件不存在 → **终止生成**，提示用户：
   ```
   ⛔ 未找到 doc/design/{module}/review-checklist.md。
   请先使用 /specflow-doc-techpack 生成 Tech Pack 和联合快审要点，
   然后完成联合快审（Stage C）后再生成模块。
   ```

> **原则**：Tech Pack → 联合快审通过 → 才能生成代码。这是工作流 Stage C → Stage D 的硬性门禁。

---

### 第 1 步：信息收集

1. 读取 Tech Pack（如提供），提取：
   - §3 数据库设计：表名、字段、类型、约束、索引、外键
   - §4 API 清单：路径、HTTP 方法、Request/Response 字段、鉴权要求
   - §4.4 错误码：编码、消息
   - §6 代码组织：模块名、包路径
2. 如果没有 Tech Pack，通过 AskUserQuestion 收集：模块名、实体名、关键字段、是否需要鉴权

### 第 2 步：生成计划

输出生成计划供用户确认，格式：

```markdown
## 模块生成计划

**模块名**: {module}
**包路径**: com.specflow.api.modules.{module}
**实体列表**: {Entity1}, {Entity2}, ...

### 将生成的文件

| 层 | 文件 | 说明 |
|----|------|------|
| domain/entity | {Entity}.java | 领域实体 |
| domain/repository | {Entity}Repository.java | 仓库接口 |
| infrastructure/persistence | {Entity}DO.java | 数据对象 |
| infrastructure/persistence | {Entity}Mapper.java | MyBatis Mapper |
| infrastructure/persistence | {Entity}RepositoryImpl.java | 仓库实现 |
| infrastructure/persistence/converter | {Entity}Converter.java | DO↔Entity 转换 |
| application | {Entity}Service.java | 应用服务 |
| interfaces | {Entity}Controller.java | REST 控制器 |
| interfaces/dto | {Action}{Entity}Request.java | 请求 DTO |
| interfaces/dto | {Entity}Response.java | 响应 DTO |
| migration | V{x.x}__{desc}.sql | Flyway 迁移 |
| test/resources | schema-h2.sql 追加 | H2 测试 Schema |

### 需要手动处理

- [ ] AuthInterceptor.EXCLUDE_PATHS — 如有公开接口，需添加路径
- [ ] Service 方法体 — TODO 处需填充业务逻辑
- [ ] 测试文件 — 使用 `/specflow-test-gen` 生成
```

### 第 3 步：代码生成

用户确认后，按以下顺序生成（自底向上，确保依赖关系正确）：

1. **Domain 层** — Entity → Repository 接口
2. **Infrastructure 层** — DO → Converter → Mapper → RepositoryImpl
3. **Application 层** — Service
4. **Interfaces 层** — DTOs → Controller
5. **数据库** — Flyway 迁移 SQL → 追加 H2 测试 Schema

每层生成时参照 `references/code-templates.md` 中的模板。

### 第 4 步：集成检查

生成完成后自动检查：

1. **编译验证** — 运行 `./mvnw compile -pl specflow-api -am` 确认编译通过
2. **Checkstyle 验证** — 运行 `./mvnw checkstyle:check -pl specflow-api` 确认风格合规
3. **文件清单** — 输出所有生成文件的路径，方便用户 review

### 第 5 步：后续建议

```markdown
## 下一步

1. 填充 Service 方法中的 TODO 业务逻辑
2. 如有公开接口，在 `AuthInterceptor.EXCLUDE_PATHS` 中添加路径
3. 运行 `/specflow-test-gen {module}` 生成测试
4. 运行 `/specflow-code-review {module}` 进行代码审核
```

## 关键规则

### 命名规范

| 类别 | 规范 | 示例 |
|------|------|------|
| 模块目录 | 小写单数 | `order`, `product`, `pet-care` → `petcare` |
| 实体类 | 大驼峰单数 | `User`, `Pet`, `CareRecord` |
| 表名 | 蛇形复数 | `users`, `pets`, `care_records` |
| API 路径 | 蛇形复数 | `/api/v1/users`, `/api/v1/care-records` |
| Request DTO | `{Action}{Entity}Request` | `CreatePetRequest`, `UpdateUserRequest` |
| Response DTO | `{Entity}Response` | `PetResponse`, `UserResponse` |

### 类型映射

| 层 | 时间类型 | 布尔类型 | 枚举类型 |
|----|---------|---------|---------|
| Domain Entity | `Instant` | primitive `boolean` | Java Enum |
| DO | `LocalDateTime` | wrapper `Boolean` | `String` |
| Request DTO | 按需（`LocalDate` 等） | 按需 | 内联 Enum |
| Response DTO | `Instant` | 按需 | 内联 Enum |
| Migration SQL | `TIMESTAMP` | `BOOLEAN` | `VARCHAR` |

### Domain 层纯净性

Domain 层不得出现任何框架注解：
- 不允许：`@Component`, `@Service`, `@Repository`, `@Autowired`, `@TableName`, `@TableId`
- 只允许：Lombok（`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`）

### 时区处理

所有 `LocalDateTime` ↔ `Instant` 转换必须使用 `ZoneId.of("UTC")`，禁止 `ZoneId.systemDefault()`。
