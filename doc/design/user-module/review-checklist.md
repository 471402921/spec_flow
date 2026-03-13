# 联合快审要点汇总 — 用户模块 P0/P1/P2

> 更新时间：2026-02-12
> 基于文档：`tech-pack-p0.md`、`tech-pack-p1.md`、`tech-pack-p2.md`
> PRD 版本：`v1.3`

---

## 1. 文档清单

| 批次 | 文档路径 | API 数量 | 新建表 | 测试用例 | 决策项 |
|------|----------|----------|--------|----------|--------|
| P0 | `tech-pack-p0.md` | 12 (API-01~12) | users, pets | TC-01~14 | D-01~D-06 |
| P1 | `tech-pack-p1.md` | 11 (API-13~23) | families, family_members, family_invitations | TC-14~29 | D-07~D-10 |
| P2 | `tech-pack-p2.md` | 7 (API-24~30) | verification_tokens + users 加字段 | TC-30~41 | D-11~D-16 |

---

## 2. 待确认决策项（共 16 项）

### P0 决策（D-01 ~ D-06）

| # | 决策项 | 建议 | 确认结果 |
|---|--------|------|----------|
| D-01 | 密码加密依赖：`spring-security-crypto` 还是完整 `spring-boot-starter-security`？ | 仅 `spring-security-crypto`，避免自动配置安全过滤链 | ✅ 已确认 |
| D-02 | 注册成功后是否自动登录（返回 token）？ | 不自动登录，引导到登录页 | ✅ 已确认 |
| D-03 | 宠物恢复流程：单接口还是拆分接口？ | 拆分：`POST /pets` 返回匹配信息 + `POST /pets/{id}/restore` 恢复 | ✅ 已确认 |
| D-04 | Session 模块是否需要重构位置？ | **已重构**：User 模块定义 `TokenProvider` 接口，Auth 模块提供实现 | ✅ 已确认（聚合根边界清晰，依赖方向正确） |
| D-05 | 头像 URL 是否需要格式校验（如必须 http/https 开头）？ | P0 暂不校验，仅存储字符串 | ✅ 已确认 |
| D-06 | 邮箱格式校验使用何种方式？ | Jakarta Validation `@Email` + 自定义正则（RFC 5322 简化版） | ✅ 已确认 |

### P1 决策（D-07 ~ D-10）

| # | 决策项 | 建议 | 确认结果 |
|---|--------|------|----------|
| D-07 | families 表是否冗余 `owner_id`？ | 建议冗余，避免每次查主人都要 JOIN family_members。转让主人时需同步更新两处。 | ✅ 已确认 |
| D-08 | 邀请码字符集 32 字符、8 位长度，是否需要加盐或更复杂的生成策略？ | 不需要。`SecureRandom` 生成 + DB 唯一性校验足够。 | ✅ 已确认 |
| D-09 | 解散家庭是否需要事件通知（通知被解散的成员）？ | P1 不实现通知，仅做数据清理。后续可加事件。 | ✅ 已确认 |
| D-10 | 宠物家庭视图 API 设计：扩展现有 `GET /pets` 还是新建独立接口？ | 新建 `GET /api/v1/families/{familyId}/pets`，现有 `GET /pets` 保持只返回用户自己的宠物。 | ✅ 已确认 |

### P2 决策（D-11 ~ D-16）

| # | 决策项 | 建议 | 确认结果 |
|---|--------|------|----------|
| D-11 | 邮件发送方案：自建 SMTP？第三方服务？先用日志模拟？ | P2 先用日志模拟（打印验证链接到日志），定义 `EmailService` 接口后续替换实现。 | ✅ 已确认 |
| D-12 | 验证令牌生成方式：UUID？JWT？自定义随机串？ | UUID（`SecureRandom` 生成的 URL-safe 随机串），不用 JWT（无需自验证，DB 查询即可）。 | ✅ 已确认 |
| D-13 | 验证链接的前端 URL 格式？ | 后端只生成 token，前端拼接完整链接。后端提供 `POST /verify-email` 接口接收 token。 | ✅ 已确认 |
| D-14 | 注销后 30 天物理清除：Spring Scheduler 还是外部调度？ | Spring `@Scheduled` 定时任务（specflow-worker 模块），每日凌晨执行。 | ✅ 已确认 |
| D-15 | 注销的用户邮箱是否释放（让其他人可注册）？ | 不释放。邮箱在软删除期间仍占用唯一约束。30 天物理清除后才释放。 | ✅ 已确认 |
| D-16 | 账号锁定计数是否需要持久化到 Redis？ | 不需要。100 万用户规模下直接用 DB 字段即可。Redis 增加复杂度，收益不大。 | ✅ 已确认 |

---

## 3. 跨批次联动点

| 联动ID | 方向 | 内容 | 影响范围 |
|--------|------|------|----------|
| CL-01 | P2 → P0 | 登录接口（API-02）增加账号锁定逻辑 | P0 的 UserService.login() 需要改造 |
| CL-02 | P2 → P1 | 创建/加入家庭（API-13, API-19）增加邮箱验证检查 | P1 的 FamilyService 需要改造 |
| CL-03 | P2 → P0 | users 表新增 3 个字段（email_verified, failed_login_attempts, locked_until） | 新增 Flyway 迁移脚本 |
| CL-04 | P0 预留 | users 表 `deleted` + `deleted_at` 为 P2 注销功能预留 | P0 建表时已包含 |

**审查重点**：P0/P1 开发时是否需要预留扩展点（如注释标记），还是 P2 开发时直接改造即可？

---

## 4. 规则覆盖完整性检查

### PRD 业务规则 → Tech Pack 映射

| 规则ID | 规则摘要 | 批次 | Tech Pack 是否覆盖 |
|--------|----------|------|-------------------|
| BR-01 | 邮箱唯一，转小写 | P0 | ✅ TC-01, TC-02 |
| BR-02 | 宠物上限 20 | P0 | ✅ TC-09 |
| BR-03 | 家庭上限 5 | P1 | ✅ TC-17, TC-24 |
| BR-04 | 家庭成员上限 10 | P1 | ✅ TC-18 |
| BR-05 | 邀请码 7 天有效，全局唯一 | P1 | ✅ TC-15, TC-16 |
| BR-06 | 宠物软删除 + 恢复 | P0 | ✅ TC-11, TC-12 |
| BR-07 | 账号注销软删除 30 天 | P2 | ✅ TC-38, TC-39 |
| BR-08 | bcrypt 加密 | P0 | ✅ TC-03, TC-04 |
| BR-09 | 宠物种类枚举 DOG/CAT | P0 | ✅ TC-08 |
| BR-10 | 宠物性别枚举 MALE/FEMALE | P0 | ✅ TC-08 |
| BR-11 | 邮箱验证链接 24h 有效 | P2 | ✅ TC-30, TC-31 |
| BR-12 | 密码重置链接 30min 有效 | P2 | ✅ TC-34, TC-35 |
| BR-13 | 5 次错误锁定 15 分钟 | P2 | ✅ TC-32, TC-33 |
| BR-14 | Session 有效期 30 天 | P0 | ✅ TC-05, TC-06 |
| BR-15 | 邮件 60s 频率限制 | P2 | ✅ TC-36 |
| BR-16 | 头像存 URL 字符串 | P0 | ✅ 无需专项测试 |
| BR-17 | 家庭解散硬删除 | P1 | ✅ TC-23 |
| BR-18 | 日期校验 UTC | P0 | ✅ TC-10 |

### PRD 验收项 → 测试用例映射

| AC-ID | 验收描述 | 批次 | 对应测试 |
|-------|----------|------|----------|
| AC-01 | 邮箱+密码注册，默认昵称 | P0 | TC-01 |
| AC-02 | 重复邮箱注册被拒 | P0 | TC-02 |
| AC-03 | 密码格式校验 | P0 | TC-03 |
| AC-04 | 登录返回 Session Token | P0 | TC-04 |
| AC-05 | 登出后 Token 失效 | P0 | TC-06 |
| AC-06 | 修改昵称和头像 | P0 | 集成测试覆盖 |
| AC-07 | 修改密码需验证当前密码 | P0 | TC-07 |
| AC-08 | 添加宠物（含枚举校验） | P0 | TC-08 |
| AC-09 | 宠物数量上限 20 | P0 | TC-09 |
| AC-10 | 仅主人可编辑/删除 | P0 | TC-13 |
| AC-11 | 软删除 + 恢复提示 | P0 | TC-11, TC-12, TC-14 |
| AC-12 | 查看宠物列表 | P0 | 集成测试覆盖 |
| AC-13 | 生日不能晚于当天（UTC） | P0 | TC-10 |
| AC-14 | 创建家庭，自动成为主人 | P1 | TC-14 |
| AC-15 | 生成邀请码，旧码失效 | P1 | TC-15 |
| AC-16 | 邀请码加入家庭 | P1 | TC-17 |
| AC-17 | 家庭数量上限 5 | P1 | TC-24 |
| AC-18 | 家庭成员上限 10 | P1 | TC-18 |
| AC-19 | 查看家庭内宠物（标注主人） | P1 | TC-25 |
| AC-20 | 主人移除成员 | P1 | TC-20 |
| AC-21 | 转让主人身份 | P1 | TC-21 |
| AC-22 | 成员退出，主人不能退出 | P1 | TC-22, TC-26 |
| AC-23 | 解散家庭，物理删除 | P1 | TC-23 |
| AC-24 | 多家庭宠物分组展示 | P1 | TC-25 (部分) |
| AC-25 | 邮箱验证流程 | P2 | TC-30 |
| AC-26 | 未验证不能创建/加入家庭 | P2 | TC-40 |
| AC-27 | 5 次错误锁定 15 分钟 | P2 | TC-32 |
| AC-28 | 密码重置，Session 失效 | P2 | TC-34 |
| AC-29 | 修改邮箱完整流程 | P2 | TC-37 |
| AC-30 | 60s 邮件频率限制 | P2 | TC-36 |
| AC-31 | 注销账号（需处理主人身份） | P2 | TC-38, TC-39 |

---

## 5. 审查检查清单

### 5.1 数据库设计

- [ ] users 表字段完整性（P0 建表 + P2 加字段的预留是否合理）
- [ ] pets 表软删除设计是否满足恢复需求
- [ ] families / family_members / family_invitations 三表关系是否清晰
- [ ] verification_tokens 表设计是否覆盖三种令牌类型
- [ ] 索引设计是否满足 100 万用户规模的查询性能

### 5.2 API 设计

- [ ] 30 个接口的路径命名是否一致（RESTful 风格）
- [ ] 鉴权要求是否正确（哪些需要登录，哪些不需要）
- [ ] 错误码是否覆盖所有异常场景，HTTP 状态码是否合理
- [ ] API-19（加入家庭）的 5 步校验顺序是否与 PRD 一致

### 5.3 跨批次一致性

- [ ] P0 建 users 表时是否预留 `deleted` + `deleted_at`（供 P2 使用）
- [ ] P0 的 API-02（登录）是否可以在 P2 无痛增加锁定逻辑
- [ ] P1 的 API-13/API-19 是否可以在 P2 无痛增加邮箱验证检查
- [ ] P2 的 Flyway 迁移脚本是否能兼容已有数据（ALTER TABLE ADD COLUMN）

### 5.4 测试覆盖

- [ ] 18 条 BR 规则全部有对应测试用例
- [ ] 31 条 AC 验收标准全部有对应测试用例
- [ ] 边界场景（必测边界）是否充分

---

## 6. 快审结论

| 日期 | 结论（通过/退回） | 关键问题 | 下一步 |
|------|--------------------|----------|--------|
| 2026-02-12 | ✅ 通过 | 16 项决策全部确认；D-04 已重构为反向依赖（User 定义 TokenProvider 接口，Auth 提供实现），聚合根边界清晰 | 进入阶段 D — AI 主导开发，按 P0 → P1 → P2 顺序开发 |

**通过后下一步**：进入阶段 D — AI 主导开发，按 P0 → P1 → P2 顺序开发。

**补充记录（2026-02-12）**：
- D-04 已重构完成：User 模块定义 `TokenProvider` 接口（`user/domain/service/`），Auth 模块提供 `SessionTokenProvider` 实现
- 依赖方向反转：支撑域（Auth）依赖核心域（User）的接口，聚合根边界清晰
- 需修复的技术债务：SEC-001, SEC-002, JAVA-001, SPRING-001, JAVA-004（见代码审核报告）

**代码修复记录（2026-02-12）**：
根据代码审核报告，已完成以下修复：

| 问题ID | 级别 | 问题描述 | 修复方案 | 状态 |
|--------|------|----------|----------|------|
| SPRING-001 | ERROR | `PetController.deletePet()` 使用 `@ResponseStatus(HttpStatus.NO_CONTENT)` 与返回 `Result` 冲突 | 移除 `@ResponseStatus` 注解，统一返回 200 + `Result` | ✅ 已修复 |
| DDD-001 | ERROR | H2 schema 的 email 唯一索引与生产环境不一致（缺少 `LOWER()`） | H2 索引改为 `CREATE UNIQUE INDEX ... ON users(LOWER(email))` | ✅ 已修复 |
| SEC-018 | WARNING | `UserService` 中硬编码 `new BCryptPasswordEncoder()` | 创建 `SecurityConfig` 配置类定义 `@Bean PasswordEncoder`，通过构造函数注入 | ✅ 已修复 |
| JAVA-001 | WARNING | `AuthInterceptor.isExcludedPath()` 未处理 null path | 添加 `path == null || path.isBlank()` 检查 | ✅ 已修复 |
| JAVA-002 | WARNING | Token 脱敏逻辑在多处重复 | 创建 `LogMasker` 工具类统一处理 | ✅ 已修复 |

**新增文件**：
- `specflow-api/src/main/java/com/specflow/api/config/SecurityConfig.java` - 安全配置类，定义 PasswordEncoder Bean
- `specflow-api/src/main/java/com/specflow/api/util/LogMasker.java` - 日志脱敏工具类

**修改文件**：
- `PetController.java` - 移除 `@ResponseStatus(HttpStatus.NO_CONTENT)`
- `schema-h2.sql` - 统一 email 唯一索引定义
- `UserService.java` - 使用注入的 PasswordEncoder，使用 LogMasker
- `SessionTokenProvider.java` - 使用 LogMasker
- `AuthInterceptor.java` - 添加 null 检查
- `UserServiceTest.java` - Mock PasswordEncoder 替代真实实例
