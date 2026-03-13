# Tech Pack - 用户模块 P0 批次（核心）

> 模块名称：`user`（`P0 - 核心`）
> 版本：`v0.1`
> 状态：`草稿`
> 更新时间：2026-02-12
> 负责人：后端主责
> 基于 PRD：`doc/requirements/user-module-prd.md v1.3`
> 前置依赖：无（首批次）

---

## 原则

Tech Pack 只记录**约束和决策**。字段设计、接口参数、测试代码等实现细节由 AI 在开发阶段根据架构规范（CLAUDE.md）自动生成，无需在此文档中预定义。

---

## 1. 输入基线

- PRD 版本：`v1.3`
- 关键规则ID范围：`BR-01, BR-02, BR-06, BR-08, BR-09, BR-10, BR-14, BR-16, BR-18`
- 验收项范围：`AC-01 ~ AC-13`
- 前置条件：项目基础设施已就绪（Maven 多模块、Docker Compose、CI、Session 模块）
- 功能范围：
  - 用户注册与登录（邮箱+密码）
  - 用户资料管理（昵称、头像）
  - 密码修改
  - 宠物 CRUD（添加、编辑、删除、恢复）
  - Session Token 鉴权
- 非目标（P0 不实现）：
  - 家庭模块（P1）
  - 邮箱验证流程、账号锁定、密码重置、邮箱修改、账号注销（P2）
  - 邮件发送功能（P2 依赖）
  - 头像上传接口（P0 仅存储 URL 字符串，上传接口待对象存储选型后实现）
- 体量假设：按 100 万用户设计，单库单表 + 合理索引。PostgreSQL 单表可承载千万级数据，无需分库分表。未来迁移到云 RDS（PostgreSQL）无需改造 schema。

---

## 2. 规则映射总表

| PRD规则ID | DB约束 | API行为 | 测试点ID | 状态 |
|----------|--------|---------|----------|------|
| BR-01 | `users.email` 唯一索引，存储前转小写 | 注册/登录时邮箱转小写后匹配 | TC-01, TC-02 | 待开发 |
| BR-02 | 应用层校验 | 添加宠物前查询用户宠物数量，>=20 拒绝 | TC-09 | 待开发 |
| BR-06 | `pets.deleted` 软删除字段 + `deleted_at` 时间戳 | 删除→软删除；添加时查询已删除同名同种类宠物 | TC-11, TC-12 | 待开发 |
| BR-08 | `users.password_hash` 存储 bcrypt 哈希 | 注册时加密，登录时 verify，修改密码时先验旧再加密新 | TC-03, TC-04 | 待开发 |
| BR-09 | `pets.species` 枚举约束 `DOG/CAT` | 请求校验枚举值 | TC-08 | 待开发 |
| BR-10 | `pets.gender` 枚举约束 `MALE/FEMALE` | 请求校验枚举值 | TC-08 | 待开发 |
| BR-14 | Session 表已存在（复用 auth 模块） | 登录成功→创建 Session，登出→revoke | TC-05, TC-06 | 待开发 |
| BR-16 | `users.avatar_url` / `pets.avatar_url` VARCHAR 存储 URL | P0 仅接收 URL 字符串，不处理上传 | - | 待开发 |
| BR-18 | 无 DB 约束 | 宠物生日校验使用 `LocalDate` + UTC 时区比较 | TC-10 | 待开发 |

---

## 3. 数据库设计（只记录决策）

### 3.1 核心实体

| 表名 | 用途 | 备注 |
|------|------|------|
| `users` | 用户账号 | 新建 |
| `pets` | 宠物档案 | 新建 |
| `session` | 登录会话 | **已存在**，复用 auth 模块，增加 `users.id` 外键关联 |

### 3.2 关键约束与决策

**users 表：**
- `email` 唯一索引（存储为小写）
- `password_hash` 存储 bcrypt，不可逆
- `avatar_url` 可为空，VARCHAR 存储 URL 字符串
- `nickname` 非空，默认为邮箱@前缀
- 软删除：使用 MyBatis-Plus `deleted` 字段（为 P2 注销功能预留）
- `deleted_at` 时间戳（为 P2 30 天清除逻辑预留）

**pets 表：**
- `owner_id` 外键关联 `users.id`，非空
- `species` 枚举：`DOG`、`CAT`（DB 存字符串，Java 用 enum）
- `gender` 枚举：`MALE`、`FEMALE`（同上）
- `birthday` 可为空，DATE 类型
- 软删除：`deleted` + `deleted_at`
- **宠物恢复查询**：按 `owner_id` + `name` + `species` + `deleted=true` 查询

**session 表（已有）：**
- 无结构变更
- `user_id` 字段已存在，将关联新 `users` 表

**决策项（需人确认）：**

| # | 决策项 | 建议 | 状态 |
|---|--------|------|------|
| D-01 | 密码加密依赖：引入 `spring-security-crypto` 还是完整 `spring-boot-starter-security`？ | 仅 `spring-security-crypto`，避免自动配置安全过滤链 | ✅ 已确认 |
| D-02 | 注册成功后是否自动登录（返回 token）？ | 不自动登录，引导到登录页 | ✅ 已确认 |
| D-03 | 宠物恢复流程：单接口（添加时自动恢复）还是拆分接口？ | 拆分：`POST /pets` 返回匹配信息 + `POST /pets/{id}/restore` 恢复 | ✅ 已确认 |
| D-04 | Session 模块是否需要重构位置？ | **已重构**：User 模块定义 `TokenProvider` 接口，Auth 模块提供 `SessionTokenProvider` 实现，实现反向依赖 | ✅ 已确认（聚合根边界清晰，依赖方向正确） |
| D-05 | 头像 URL 是否需要格式校验（如必须 http/https 开头）？ | P0 暂不校验，仅存储字符串 | ✅ 已确认 |
| D-06 | 邮箱格式校验使用何种方式？ | Jakarta Validation `@Email` + 自定义正则（RFC 5322 简化版） | ✅ 已确认 |

---

## 4. API 设计（只记录清单与约束）

### 4.1 接口清单

| API-ID | 方法 | 路径 | 用途 | 鉴权 | 优先级 |
|--------|------|------|------|------|--------|
| API-01 | POST | `/api/v1/users/register` | 用户注册 | 否 | P0 |
| API-02 | POST | `/api/v1/users/login` | 用户登录 | 否 | P0 |
| API-03 | POST | `/api/v1/users/logout` | 用户登出 | 是 | P0 |
| API-04 | GET | `/api/v1/users/me` | 获取当前用户信息 | 是 | P0 |
| API-05 | PUT | `/api/v1/users/me` | 修改用户资料（昵称、头像） | 是 | P0 |
| API-06 | PUT | `/api/v1/users/me/password` | 修改密码 | 是 | P0 |
| API-07 | POST | `/api/v1/pets` | 添加宠物 | 是 | P0 |
| API-08 | GET | `/api/v1/pets` | 查看我的宠物列表 | 是 | P0 |
| API-09 | GET | `/api/v1/pets/{petId}` | 查看宠物详情 | 是 | P0 |
| API-10 | PUT | `/api/v1/pets/{petId}` | 编辑宠物 | 是 | P0 |
| API-11 | DELETE | `/api/v1/pets/{petId}` | 删除宠物（软删除） | 是 | P0 |
| API-12 | POST | `/api/v1/pets/{petId}/restore` | 恢复已删除宠物 | 是 | P0 |

### 4.2 关键约束（逐条，只写决策项）

#### API-01 `用户注册`

- 幂等策略：不需要（邮箱唯一约束天然防重）
- 规则映射：BR-01, BR-08, AC-01, AC-02, AC-03
- 特殊约束：
  - 邮箱转小写后存储
  - 密码 bcrypt 加密
  - 昵称不填时默认为邮箱@前缀
  - **注册成功不自动登录**（前端注册后引导到登录页）

#### API-02 `用户登录`

- 幂等策略：不需要（每次生成新 Session Token）
- 规则映射：BR-01, BR-08, BR-14, AC-04
- 特殊约束：
  - 邮箱转小写后匹配
  - bcrypt verify 密码
  - 成功后调用 `TokenProvider.createToken(userId)` 创建 Token
  - 返回 `token` 和 `expiredAt`
  - P0 不实现账号锁定（P2）

#### API-03 `用户登出`

- 规则映射：BR-14, AC-05
- 特殊约束：
  - 从请求头 `Authorization: Bearer {token}` 获取 token
  - 调用 `TokenProvider.revokeToken(token)` 使当前 token 失效

#### API-04 `获取当前用户信息`

- 规则映射：AC-06
- 特殊约束：
  - 通过 token 认证后返回当前用户信息
  - 不返回 `password_hash` 字段

#### API-05 `修改用户资料`

- 规则映射：AC-06, BR-16
- 特殊约束：
  - 仅允许修改 `nickname`（2-20字符）和 `avatarUrl`
  - 不允许修改 email

#### API-06 `修改密码`

- 规则映射：BR-08, AC-07
- 特殊约束：
  - 必须验证当前密码（bcrypt verify）
  - 新密码规则：至少 8 位，包含字母+数字
  - **修改成功后不失效其他 Session**（区别于密码重置 P2）

#### API-07 `添加宠物`

- 幂等策略：不需要
- 规则映射：BR-02, BR-06, BR-09, BR-10, BR-18, AC-08, AC-09, AC-11, AC-13
- 特殊约束：
  - 先检查用户宠物数量是否达上限（20）
  - 检查已删除宠物中是否有同名同种类的
  - 如有匹配，返回特殊响应码和匹配列表，**不自动创建**
  - 如无匹配，正常创建
  - 生日校验：不能晚于当天（UTC）

#### API-11 `删除宠物`

- 规则映射：BR-06, AC-10, AC-11
- 特殊约束：
  - 仅宠物主人（`owner_id == 当前用户`）可删除
  - 软删除：设置 `deleted=true`，记录 `deleted_at`

#### API-12 `恢复已删除宠物`

- 规则映射：BR-06, AC-11
- 特殊约束：
  - 仅宠物主人可恢复
  - 恢复时需检查宠物数量上限（恢复后不应超过 20）
  - 取消软删除标记，清除 `deleted_at`

### 4.3 鉴权方案

**P0 鉴权方式：**
- 请求头 `Authorization: Bearer {session_token}`
- 通过 Spring Interceptor 实现（非 Spring Security Filter）
- Interceptor 流程：
  1. 提取 token
  2. 调用 `TokenProvider.getUserIdByToken(token)` 验证有效性并获取用户 ID
  3. 将 `userId` 存入请求属性（`request.setAttribute`），供 Controller 使用
  4. 无效 token → 返回 401
- 排除路径：`/api/v1/users/register`、`/api/v1/users/login`、`/actuator/**`、`/swagger-ui/**`、`/api-docs/**`

**架构说明：**
- `TokenProvider` 接口定义在 `user` 模块（核心域）
- `SessionTokenProvider` 实现在 `auth` 模块（支撑域）
- 避免 `user` 模块直接依赖 `auth` 模块的具体类

### 4.4 错误码规范

| 错误码 | HTTP状态 | 语义 | 用户提示 |
|--------|----------|------|----------|
| `EMAIL_ALREADY_EXISTS` | 409 | 邮箱已注册 | 该邮箱已被注册 |
| `INVALID_CREDENTIALS` | 401 | 邮箱或密码错误 | 邮箱或密码错误 |
| `INVALID_PASSWORD_FORMAT` | 400 | 密码格式不符合要求 | 密码至少8位，需包含字母和数字 |
| `INVALID_EMAIL_FORMAT` | 400 | 邮箱格式错误 | 请输入有效的邮箱地址 |
| `PET_LIMIT_EXCEEDED` | 400 | 宠物数量已达上限 | 已达宠物数量上限（最多20只） |
| `PET_NOT_FOUND` | 404 | 宠物不存在 | 宠物不存在 |
| `PET_ACCESS_DENIED` | 403 | 无权操作此宠物 | 仅宠物主人可执行此操作 |
| `PET_DELETED_MATCH_FOUND` | 200 | 发现已删除的同名同种类宠物 | 发现已删除的同名宠物，是否恢复？ |
| `INVALID_BIRTHDAY` | 400 | 生日不能晚于今天 | 生日不能晚于今天 |
| `INCORRECT_PASSWORD` | 400 | 当前密码错误（修改密码时） | 当前密码错误 |
| `NICKNAME_LENGTH_INVALID` | 400 | 昵称长度不符 | 昵称长度需在2-20个字符之间 |

---

## 5. 测试清单

### 5.1 核心测试点

| TC-ID | 类型 | 场景描述 | 覆盖规则ID | 预期结果 |
|-------|------|----------|------------|----------|
| TC-01 | 集成 | 使用有效邮箱+密码注册 | BR-01, BR-08 | 201，用户创建成功，密码为 bcrypt 哈希 |
| TC-02 | 集成 | 使用已注册邮箱（含大小写变体）注册 | BR-01 | 409，`EMAIL_ALREADY_EXISTS` |
| TC-03 | 单测 | 密码格式校验（<8位/纯字母/纯数字） | BR-08 | 400，`INVALID_PASSWORD_FORMAT` |
| TC-04 | 集成 | 使用正确邮箱+密码登录 | BR-08, BR-14 | 200，返回 token 和 expiredAt |
| TC-05 | 集成 | 使用错误密码登录 | BR-08 | 401，`INVALID_CREDENTIALS` |
| TC-06 | 集成 | 登出后使用旧 token 访问 | BR-14 | 401 |
| TC-07 | 单测 | 修改密码（旧密码错误） | BR-08 | 400，`INCORRECT_PASSWORD` |
| TC-08 | 集成 | 添加宠物（有效数据，含枚举校验） | BR-09, BR-10 | 201，宠物创建成功 |
| TC-09 | 单测 | 用户已有 20 只宠物时添加 | BR-02 | 400，`PET_LIMIT_EXCEEDED` |
| TC-10 | 单测 | 宠物生日晚于今天（UTC） | BR-18 | 400，`INVALID_BIRTHDAY` |
| TC-11 | 集成 | 删除宠物后查询不可见 | BR-06 | 软删除成功，列表中不再出现 |
| TC-12 | 集成 | 添加与已删除宠物同名同种类的宠物 | BR-06 | 返回匹配列表，提示可恢复 |
| TC-13 | 集成 | 非主人尝试编辑/删除宠物 | - | 403，`PET_ACCESS_DENIED` |
| TC-14 | 集成 | 恢复已删除宠物 | BR-06 | 宠物恢复，列表中重新出现 |

### 5.2 必测边界

- 邮箱大小写（`User@Example.COM` 与 `user@example.com` 视为同一用户）
- 昵称长度边界（1字符/2字符/20字符/21字符）
- 宠物名字长度边界（0字符/1字符/30字符/31字符）
- 品种文本长度边界（0字符/1字符/50字符/51字符）
- 无效枚举值（species=`BIRD`，gender=`OTHER`）
- 宠物数量边界（第19只/第20只/第21只）
- 空 token / 过期 token / 被 revoke 的 token 访问受保护接口
- 已删除宠物的恢复后再删除再恢复

### 5.3 质量门禁

- checkstyle：通过
- 全量测试：通过
- 核心验收用例（AC-01 ~ AC-13）：通过

---

## 6. 代码组织（模块结构）

```
specflow-api/src/main/java/com/specflow/api/
├── modules/
│   ├── auth/                          # 已有（Session 模块，实现 TokenProvider）
│   │   └── application/
│   │       └── SessionTokenProvider.java  # 新增：TokenProvider 实现
│   └── user/                          # 新建
│       ├── interfaces/
│       │   ├── UserController.java
│       │   ├── PetController.java
│       │   └── dto/
│       ├── application/
│       │   ├── UserService.java
│       │   └── PetService.java
│       ├── domain/
│       │   ├── entity/
│       │   │   ├── User.java
│       │   │   └── Pet.java
│       │   ├── repository/
│       │   │   ├── UserRepository.java
│       │   │   └── PetRepository.java
│       │   └── service/
│       │       └── TokenProvider.java   # 新增：Token 提供者接口（反向依赖）
│       └── infrastructure/
│           └── persistence/
│               ├── UserDO.java
│               ├── UserMapper.java
│               ├── UserRepositoryImpl.java
│               ├── PetDO.java
│               ├── PetMapper.java
│               ├── PetRepositoryImpl.java
│               └── converter/
│                   ├── UserConverter.java
│                   └── PetConverter.java
├── config/
│   ├── AuthInterceptor.java           # 新建：Token 鉴权拦截器
│   └── WebConfig.java                 # 修改：注册 AuthInterceptor
```

---

## 7. 快审记录（90 分钟会议）

| 日期 | 结论（通过/退回） | 关键问题 | Owner | 截止时间 |
|------|--------------------|----------|-------|----------|
| - | - | - | - | - |

---

## 8. 变更联动

| 变更ID | 来源 | 变更摘要 | 对 Tech Pack 影响 | 状态 |
|--------|------|----------|-------------------|------|
| CL-01 | P2 → P0 | 登录接口（API-02）增加账号锁定逻辑 | UserService.login() 需改造 | 待 P2 开发时实施 |
| CL-03 | P2 → P0 | users 表增加 3 个字段（email_verified, failed_login_attempts, locked_until） | 新增 Flyway 迁移脚本 | 待 P2 开发时实施 |
| CL-04 | P0 预留 | users 表 `deleted` + `deleted_at` 为 P2 注销功能预留 | P0 建表时已包含 | 已就绪 |
