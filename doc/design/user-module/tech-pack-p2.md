# Tech Pack - 用户模块 P2 批次（安全增强）

> 模块名称：`user`（邮箱验证、账号锁定、密码重置、邮箱修改、账号注销）
> 版本：`v0.1`
> 状态：`草稿`
> 更新时间：2026-02-12
> 负责人：后端主责
> 基于 PRD：`doc/requirements/user-module-prd.md v1.3`
> 前置依赖：`tech-pack-p0.md`（用户模块）+ `tech-pack-p1.md`（家庭模块）

---

## 原则

Tech Pack 只记录**约束和决策**。字段设计、接口参数、测试代码等实现细节由 AI 在开发阶段根据架构规范（CLAUDE.md）自动生成，无需在此文档中预定义。

---

## 1. 输入基线

- PRD 版本：`v1.3`
- 关键规则ID范围：`BR-07, BR-11, BR-12, BR-13, BR-15`
- 验收项范围：`AC-25 ~ AC-31`
- 前置条件：P0 + P1 已完成（users 表、pets 表、families 相关表、Session 鉴权已就绪）
- 功能范围：
  - 邮箱验证流程（注册后验证）
  - 账号锁定（连续密码错误）
  - 密码重置（通过邮件链接）
  - 邮箱修改（需验证当前密码 + 新邮箱验证）
  - 账号注销（软删除）
- 非目标（P2 不实现）：
  - 实际邮件发送集成（P2 定义接口和流程，但邮件发送可先用日志模拟，实际邮件服务选型单独决策）
- 体量假设：同 P0，100 万用户。

---

## 2. 规则映射总表

| PRD规则ID | DB约束 | API行为 | 测试点ID | 状态 |
|----------|--------|---------|----------|------|
| BR-07 | `users.deleted` + `users.deleted_at`（P0 已预留） | 注销→软删除，30 天后定时任务物理清除 | TC-38, TC-39 | 待开发 |
| BR-11 | `verification_tokens` 表，`used` + `expired_at` | 邮箱验证链接 24h 有效，一次性使用 | TC-30, TC-31 | 待开发 |
| BR-12 | `verification_tokens` 表，`used` + `expired_at` | 密码重置链接 30min 有效，一次性使用 | TC-34, TC-35 | 待开发 |
| BR-13 | `users.failed_login_attempts` + `users.locked_until` | 5 次错误锁定 15 分钟，成功后重置 | TC-32, TC-33 | 待开发 |
| BR-15 | 应用层校验（基于 `verification_tokens.created_at`） | 同一邮箱 60s 内不能重复发送 | TC-36 | 待开发 |

---

## 3. 数据库设计（只记录决策）

### 3.1 核心实体

| 表名 | 用途 | 备注 |
|------|------|------|
| `verification_tokens` | 邮箱验证/密码重置/邮箱修改的令牌 | 新建，统一管理所有验证令牌 |
| `users`（修改） | 增加账号锁定相关字段 | P0 已建，P2 加字段 |

### 3.2 关键约束与决策

**users 表新增字段：**
- `email_verified`：boolean，默认 false。邮箱是否已验证
- `failed_login_attempts`：int，默认 0。连续失败登录次数
- `locked_until`：timestamp，可为空。账号锁定截止时间

**verification_tokens 表：**
- `token`：唯一索引，VARCHAR(128)，用于 URL 中的令牌
- `user_id`：关联用户
- `type`：枚举 `EMAIL_VERIFICATION`、`PASSWORD_RESET`、`EMAIL_CHANGE`
- `email`：目标邮箱（用于邮箱修改时存储新邮箱）
- `used`：boolean，默认 false
- `expired_at`：过期时间
- `created_at`：创建时间（用于 60s 频率限制检查）
- 不做软删除，过期+已使用的令牌可由定时任务清理

**P0 改造（P2 实施时）：**
- 登录接口（API-02）增加锁定检查逻辑
- 创建/加入家庭接口（API-13, API-19）增加邮箱验证检查

**决策项（需人确认）：**

| # | 决策项 | 建议 | 状态 |
|---|--------|------|------|
| D-11 | 邮件发送方案：自建 SMTP？第三方邮件服务（SendGrid、AWS SES）？先用日志模拟？ | P2 先用日志模拟（打印验证链接到日志），邮件服务选型单独评审。定义 `EmailService` 接口，后续替换实现。 | 待确认 |
| D-12 | 验证令牌生成方式：UUID？JWT？自定义随机串？ | UUID（`SecureRandom` 生成的 URL-safe 随机串），不用 JWT（无需自验证，DB 查询即可）。 | 待确认 |
| D-13 | 验证链接的前端 URL 格式？ | 后端只生成 token，前端拼接完整链接。后端提供 `POST /verify-email` 接口接收 token。 | 待确认 |
| D-14 | 注销后 30 天物理清除：定时任务（Spring Scheduler）还是依赖外部调度？ | 使用 Spring `@Scheduled` 定时任务（soulpal-worker 模块），每日凌晨执行。 | 待确认 |
| D-15 | 注销的用户邮箱是否释放（让其他人可注册）？ | 不释放。邮箱在软删除期间仍占用唯一约束。30 天物理清除后才释放。 | 待确认 |
| D-16 | 账号锁定计数是否需要持久化到 Redis？ | 不需要。100 万用户规模下，直接用 DB 字段（`failed_login_attempts`）即可。Redis 引入增加复杂度，收益不大。 | 待确认 |

---

## 4. API 设计（只记录清单与约束）

### 4.1 接口清单

| API-ID | 方法 | 路径 | 用途 | 鉴权 | 优先级 |
|--------|------|------|------|------|--------|
| API-24 | POST | `/api/v1/users/send-verification-email` | 发送/重发邮箱验证邮件 | 是 | P2 |
| API-25 | POST | `/api/v1/users/verify-email` | 验证邮箱（通过 token） | 否 | P2 |
| API-26 | POST | `/api/v1/users/forgot-password` | 请求密码重置邮件 | 否 | P2 |
| API-27 | POST | `/api/v1/users/reset-password` | 重置密码（通过 token + 新密码） | 否 | P2 |
| API-28 | POST | `/api/v1/users/me/change-email` | 请求修改邮箱（发送验证到新邮箱） | 是 | P2 |
| API-29 | POST | `/api/v1/users/confirm-email-change` | 确认修改邮箱（通过 token） | 否 | P2 |
| API-30 | POST | `/api/v1/users/me/deactivate` | 注销/停用账号 | 是 | P2 |

### P0/P1 接口改造

| 原接口 | 改造内容 |
|--------|----------|
| API-02 `登录` | 增加：检查 `locked_until`，失败时递增 `failed_login_attempts`，成功时重置为 0 |
| API-13 `创建家庭` | 增加：检查 `email_verified = true` |
| API-19 `加入家庭` | 增加：检查 `email_verified = true` |

### 4.2 关键约束

#### API-24 `发送验证邮件`

- 规则映射：BR-11, BR-15, AC-25
- 特殊约束：
  - 已认证用户才能调用（需登录）
  - 如果邮箱已验证，返回成功（幂等，不报错）
  - 60s 频率限制：查询该用户最近一条 `EMAIL_VERIFICATION` 类型 token 的 `created_at`
  - 调用 `EmailService` 发送验证链接

#### API-25 `验证邮箱`

- 规则映射：BR-11, AC-25
- 特殊约束：
  - 无需登录（用户从邮件链接点击）
  - 校验 token 存在、未使用、未过期
  - 验证成功：设置 `users.email_verified = true`，标记 token `used = true`

#### API-26 `请求密码重置`

- 规则映射：BR-12, BR-15, AC-28
- 特殊约束：
  - 无需登录
  - **无论邮箱是否注册，都返回成功**（防枚举攻击）
  - 如果邮箱已注册：生成 token，发送重置邮件
  - 如果邮箱未注册：静默返回成功，不发邮件
  - 60s 频率限制

#### API-27 `重置密码`

- 规则映射：BR-12, AC-28
- 特殊约束：
  - 无需登录
  - 校验 token 有效（存在、未使用、未过期、type=PASSWORD_RESET）
  - 新密码规则同注册（至少 8 位，包含字母+数字）
  - **重置成功后，revoke 该用户所有 Session**（强制所有设备重新登录）
  - 标记 token `used = true`

#### API-28 `请求修改邮箱`

- 规则映射：BR-11, AC-29
- 特殊约束：
  - 需登录
  - 需验证当前密码
  - 检查新邮箱是否已被占用
  - 生成 `EMAIL_CHANGE` 类型 token，token 中记录新邮箱地址
  - 发送验证链接到新邮箱

#### API-29 `确认修改邮箱`

- 规则映射：BR-11, AC-29
- 特殊约束：
  - 无需登录（从邮件链接点击）
  - 校验 token 有效
  - 再次检查新邮箱是否已被占用（防止 token 生成后邮箱被他人注册）
  - 更新 `users.email` = 新邮箱（转小写）
  - 标记 token `used = true`

#### API-30 `注销账号`

- 规则映射：BR-07, AC-31
- 特殊约束：
  - 需验证当前密码
  - **前置检查**：用户是否为任何家庭的主人。如是，拒绝并返回这些家庭的列表
  - 注销操作（同一事务）：
    1. users 标记软删除（`deleted=true`, `deleted_at=now`）
    2. pets 批量标记软删除
    3. 退出所有家庭（物理删除 family_members 记录）
    4. revoke 所有 Session
  - 30 天后定时任务物理清除用户和宠物数据

### 4.3 错误码规范

| 错误码 | HTTP状态 | 语义 | 用户提示 |
|--------|----------|------|----------|
| `ACCOUNT_LOCKED` | 403 | 账号已锁定 | 账号已锁定，请{X}分钟后再试 |
| `EMAIL_NOT_VERIFIED` | 403 | 邮箱未验证 | 请先完成邮箱验证 |
| `EMAIL_ALREADY_VERIFIED` | 200 | 邮箱已验证（幂等返回） | 邮箱已验证 |
| `TOKEN_INVALID` | 400 | 验证令牌无效或不存在 | 链接无效 |
| `TOKEN_EXPIRED` | 400 | 验证令牌已过期 | 链接已过期，请重新申请 |
| `TOKEN_ALREADY_USED` | 400 | 验证令牌已使用 | 该链接已被使用 |
| `EMAIL_SEND_RATE_LIMITED` | 429 | 邮件发送频率限制 | 请60秒后再试 |
| `OWNER_FAMILIES_EXIST` | 400 | 用户仍是家庭主人 | 请先处理以下家庭的主人身份：{家庭列表} |
| `NEW_EMAIL_ALREADY_EXISTS` | 409 | 新邮箱已被占用 | 该邮箱已被其他账号使用 |

---

## 5. 测试清单

### 5.1 核心测试点

| TC-ID | 类型 | 场景描述 | 覆盖规则ID | 预期结果 |
|-------|------|----------|------------|----------|
| TC-30 | 集成 | 发送验证邮件 → 使用 token 验证邮箱成功 | BR-11 | email_verified = true |
| TC-31 | 单测 | 使用过期/已用的验证 token | BR-11 | 400，TOKEN_EXPIRED / TOKEN_ALREADY_USED |
| TC-32 | 集成 | 连续 5 次错误密码登录后账号锁定 | BR-13 | 第 6 次返回 403，ACCOUNT_LOCKED |
| TC-33 | 单测 | 锁定期过后可正常登录，成功登录重置计数 | BR-13 | 登录成功，failed_login_attempts = 0 |
| TC-34 | 集成 | 请求密码重置 → 使用 token 重置密码 → 旧 Session 全部失效 | BR-12 | 密码更新，所有 Session revoked |
| TC-35 | 单测 | 未注册邮箱请求密码重置 | BR-12 | 200（静默成功），不发邮件 |
| TC-36 | 单测 | 60s 内重复发送验证/重置邮件 | BR-15 | 429，EMAIL_SEND_RATE_LIMITED |
| TC-37 | 集成 | 修改邮箱完整流程（请求 → 验证 → 生效） | BR-11 | 邮箱更新，旧邮箱不可登录 |
| TC-38 | 集成 | 注销账号（非家庭主人）完整流程 | BR-07 | 用户+宠物软删除，退出所有家庭，Session 失效 |
| TC-39 | 单测 | 家庭主人尝试注销被拒，返回家庭列表 | BR-07 | 400，OWNER_FAMILIES_EXIST + 家庭列表 |
| TC-40 | 集成 | 未验证邮箱的用户尝试创建/加入家庭 | BR-11 | 403，EMAIL_NOT_VERIFIED |
| TC-41 | 单测 | 修改邮箱后新邮箱已被占用（并发场景） | - | 409，NEW_EMAIL_ALREADY_EXISTS |

### 5.2 必测边界

- 锁定计时边界（锁定后恰好 15 分钟时是否可登录）
- 验证链接恰好 24 小时时是否过期
- 重置链接恰好 30 分钟时是否过期
- 邮件频率恰好 60 秒时是否可发送
- 注销用户的数据在 30 天内不可被新注册覆盖
- 多种类型 token 混用（EMAIL_VERIFICATION token 不能用于 PASSWORD_RESET）
- 账号注销后用已有 token 验证邮箱（应失败）

### 5.3 质量门禁

- checkstyle：通过
- 全量测试（含 P0 + P1）：通过
- 核心验收用例（AC-25 ~ AC-31）：通过

---

## 6. 代码组织

P2 的代码主要是对 P0 `user` 模块的扩展，不新建模块：

```
soulpal-api/src/main/java/com/soulpal/api/modules/
├── user/
│   ├── interfaces/
│   │   ├── UserController.java              # 修改：增加 verify-email、forgot-password 等端点
│   │   └── dto/                             # 新增相关 Request/Response DTO
│   ├── application/
│   │   ├── UserService.java                 # 修改：增加邮箱验证、密码重置、注销逻辑
│   │   └── EmailService.java               # 新建：邮件发送接口
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── User.java                    # 修改：增加 emailVerified、锁定相关字段和方法
│   │   │   └── VerificationToken.java       # 新建
│   │   └── repository/
│   │       └── VerificationTokenRepository.java  # 新建
│   └── infrastructure/
│       └── persistence/
│           ├── VerificationTokenDO.java      # 新建
│           ├── VerificationTokenMapper.java  # 新建
│           ├── VerificationTokenRepositoryImpl.java  # 新建
│           ├── converter/
│           │   └── VerificationTokenConverter.java   # 新建
│           └── email/
│               └── LogEmailService.java      # 新建：日志模拟邮件发送（后续替换）

soulpal-worker/src/main/java/com/soulpal/worker/
└── scheduler/
    └── AccountCleanupScheduler.java          # 新建：30天过期数据清理定时任务
```

### 对 P0/P1 的改造点

| 文件 | 改造内容 |
|------|----------|
| `UserService.java` (P0) | 登录方法增加锁定检查和失败计数逻辑 |
| `FamilyService.java` (P1) | 创建/加入家庭增加 `email_verified` 前置检查 |
| `AuthInterceptor.java` (P0) | 增加软删除用户的 token 拒绝逻辑 |
| `users` 表 (P0) | Flyway 迁移增加 `email_verified`、`failed_login_attempts`、`locked_until` 字段 |

---

## 7. 快审记录

| 日期 | 结论 | 关键问题 | Owner | 截止时间 |
|------|------|----------|-------|----------|
| - | - | - | - | - |

---

## 8. 变更联动

| 变更ID | 来源 | 变更摘要 | 对 Tech Pack 影响 | 状态 |
|--------|------|----------|-------------------|------|
| CL-01 | P2 → P0 | 登录接口增加账号锁定逻辑 | P0 的 API-02 需要改造 | 待 P2 开发时实施 |
| CL-02 | P2 → P1 | 创建/加入家庭增加邮箱验证检查 | P1 的 API-13, API-19 需要改造 | 待 P2 开发时实施 |
| CL-03 | P2 → P0 | users 表增加 3 个字段 | 新增 Flyway 迁移脚本 | 待 P2 开发时实施 |
