# Tech Pack - 用户模块 P1 批次（家庭管理）

> 模块名称：`family`（家庭创建、邀请、成员管理、宠物家庭视图）
> 版本：`v0.1`
> 状态：`草稿`
> 更新时间：2026-02-12
> 负责人：后端主责
> 基于 PRD：`doc/requirements/user-module-prd.md v1.3`
> 前置依赖：`tech-pack-p0.md`（用户和宠物模块已实现）

---

## 原则

Tech Pack 只记录**约束和决策**。字段设计、接口参数、测试代码等实现细节由 AI 在开发阶段根据架构规范（CLAUDE.md）自动生成，无需在此文档中预定义。

---

## 1. 输入基线

- PRD 版本：`v1.3`
- 关键规则ID范围：`BR-03, BR-04, BR-05, BR-17`
- 验收项范围：`AC-14 ~ AC-24`
- 前置条件：P0 批次已完成（users 表、pets 表、Session 鉴权拦截器已就绪）
- 非目标（P1 不实现）：
  - 邮箱验证前置检查（P2，P1 阶段所有用户均可创建/加入家庭）
  - 账号注销时的家庭主人检查（P2）
- 体量假设：同 P0，100 万用户。预估家庭数 ~50 万，家庭成员关系 ~200 万行。

---

## 2. 规则映射总表

| PRD规则ID | DB约束 | API行为 | 测试点ID | 状态 |
|----------|--------|---------|----------|------|
| BR-03 | 应用层校验 | 加入/创建家庭前查询用户家庭数量，>=5 拒绝 | TC-17, TC-24 | 待开发 |
| BR-04 | 应用层校验 | 加入家庭前查询家庭成员数量，>=10 拒绝 | TC-18 | 待开发 |
| BR-05 | `family_invitations.code` 唯一索引 + `expired_at` | 生成新码时旧码失效；校验有效期 | TC-15, TC-16 | 待开发 |
| BR-17 | 无 FK 级联（应用层处理） | 解散家庭→物理删除家庭、成员关系、邀请码 | TC-23 | 待开发 |

---

## 3. 数据库设计（只记录决策）

### 3.1 核心实体

| 表名 | 用途 | 备注 |
|------|------|------|
| `families` | 家庭信息 | 新建 |
| `family_members` | 家庭与用户的多对多关系 | 新建，含角色（OWNER/MEMBER） |
| `family_invitations` | 家庭邀请码 | 新建 |

### 3.2 关键约束与决策

**families 表：**
- `name` 非空，2-20 字符
- 不做软删除，解散时硬删除（BR-17）
- `owner_id` 冗余字段，指向当前家庭主人（便于快速查询，与 `family_members` 中的角色保持一致）

**family_members 表：**
- 联合唯一索引：`(family_id, user_id)`（一个用户在同一家庭只有一条记录）
- `role` 枚举：`OWNER`、`MEMBER`
- 不做软删除，退出/移除时硬删除
- 解散家庭时，按 `family_id` 批量物理删除

**family_invitations 表：**
- `code` 唯一索引（全局唯一）
- `family_id` 索引
- `expired_at` 有效期字段
- 生成新邀请码时，先将该家庭所有未过期邀请码标记为失效（`revoked = true`），再插入新码
- 不做软删除，解散家庭时按 `family_id` 批量物理删除
- 邀请码可多次使用（不是一次性的），直到过期、被撤销或家庭满员

**决策项（需人确认）：**

| # | 决策项 | 建议 | 状态 |
|---|--------|------|------|
| D-07 | families 表是否冗余 `owner_id`？ | 建议冗余，避免每次查主人都要 JOIN family_members。转让主人时需要同步更新两处。 | 待确认 |
| D-08 | 邀请码字符集 32 个字符、8 位长度，理论组合 ~1 万亿，是否需要加盐或更复杂的生成策略？ | 不需要。100 万用户规模下，`SecureRandom` 生成 + DB 唯一性校验足够。 | 待确认 |
| D-09 | 解散家庭是否需要事件通知（如通知被解散的成员）？ | P1 不实现通知，仅做数据清理。后续可加事件。 | 待确认 |
| D-10 | 宠物家庭视图（AC-24）的 API 设计：是在现有 `GET /pets` 上扩展，还是新建独立接口？ | 新建 `GET /api/v1/families/{familyId}/pets` 接口，专门查看某个家庭内的宠物。`GET /pets` 保持只返回用户自己的宠物。 | 待确认 |

---

## 4. API 设计（只记录清单与约束）

### 4.1 接口清单

| API-ID | 方法 | 路径 | 用途 | 鉴权 | 优先级 |
|--------|------|------|------|------|--------|
| API-13 | POST | `/api/v1/families` | 创建家庭 | 是 | P1 |
| API-14 | GET | `/api/v1/families` | 查看我加入的家庭列表 | 是 | P1 |
| API-15 | GET | `/api/v1/families/{familyId}` | 查看家庭详情（含成员列表） | 是 | P1 |
| API-16 | PUT | `/api/v1/families/{familyId}` | 修改家庭名称 | 是 | P1 |
| API-17 | DELETE | `/api/v1/families/{familyId}` | 解散家庭 | 是 | P1 |
| API-18 | POST | `/api/v1/families/{familyId}/invitations` | 生成邀请码 | 是 | P1 |
| API-19 | POST | `/api/v1/families/join` | 通过邀请码加入家庭 | 是 | P1 |
| API-20 | DELETE | `/api/v1/families/{familyId}/members/{userId}` | 移除成员 | 是 | P1 |
| API-21 | POST | `/api/v1/families/{familyId}/members/leave` | 退出家庭 | 是 | P1 |
| API-22 | POST | `/api/v1/families/{familyId}/transfer` | 转让主人身份 | 是 | P1 |
| API-23 | GET | `/api/v1/families/{familyId}/pets` | 查看家庭内所有成员的宠物 | 是 | P1 |

### 4.2 关键约束

#### API-13 `创建家庭`

- 规则映射：BR-03, AC-14
- 特殊约束：
  - 查询用户已加入的家庭数，>=5 时拒绝
  - 创建 family 记录 + 创建 family_members 记录（role=OWNER）需在同一事务
  - P1 不检查邮箱验证状态（P2 加）

#### API-18 `生成邀请码`

- 规则映射：BR-05, AC-15
- 特殊约束：
  - 仅家庭主人可操作
  - 邀请码 8 位，字符集：`ABCDEFGHJKLMNPQRSTUVWXYZ23456789`（排除 0/O/I/1）
  - 使用 `SecureRandom` 生成
  - 生成前先撤销该家庭所有未过期邀请码
  - 有效期 7 天
  - DB 唯一性校验，冲突时重新生成（最多 3 次重试）

#### API-19 `通过邀请码加入家庭`

- 规则映射：BR-03, BR-04, BR-05, AC-16, AC-17, AC-18
- 特殊约束：
  - 邀请码输入自动转大写
  - 校验顺序（严格按 PRD 定义）：
    1. 邀请码不存在 → `INVITATION_NOT_FOUND`
    2. 邀请码已过期/已撤销 → `INVITATION_EXPIRED`
    3. 用户已是该家庭成员 → `ALREADY_FAMILY_MEMBER`
    4. 用户家庭数已达 5 → `FAMILY_LIMIT_EXCEEDED`
    5. 家庭成员数已达 10 → `FAMILY_MEMBER_LIMIT_EXCEEDED`

#### API-17 `解散家庭`

- 规则映射：BR-17, AC-23
- 特殊约束：
  - 仅家庭主人可操作
  - 同一事务内物理删除：family_invitations（by family_id）→ family_members（by family_id）→ families（by id）
  - 不影响任何用户或宠物数据

#### API-22 `转让主人身份`

- 规则映射：AC-21
- 特殊约束：
  - 仅当前家庭主人可操作
  - 目标用户必须是该家庭的现有成员
  - 同一事务内：原主人 role → MEMBER，目标用户 role → OWNER，families.owner_id → 目标用户
  - 三条数据更新需原子性

#### API-23 `查看家庭内宠物`

- 规则映射：AC-19, AC-24
- 特殊约束：
  - 仅家庭成员可访问
  - 查询逻辑：获取家庭所有成员 → 查询这些成员的所有未删除宠物
  - 返回数据需标注每只宠物的主人昵称和主人 ID

### 4.3 错误码规范

| 错误码 | HTTP状态 | 语义 | 用户提示 |
|--------|----------|------|----------|
| `FAMILY_LIMIT_EXCEEDED` | 400 | 已达家庭数量上限 | 已达加入家庭数量上限（最多5个） |
| `FAMILY_MEMBER_LIMIT_EXCEEDED` | 400 | 家庭成员已满 | 家庭成员已满（最多10人） |
| `FAMILY_NOT_FOUND` | 404 | 家庭不存在 | 家庭不存在 |
| `FAMILY_ACCESS_DENIED` | 403 | 无权操作此家庭 | 仅家庭主人可执行此操作 |
| `FAMILY_NAME_INVALID` | 400 | 家庭名称不符合规则 | 家庭名称长度需在2-20个字符之间 |
| `INVITATION_NOT_FOUND` | 400 | 邀请码无效 | 邀请码无效 |
| `INVITATION_EXPIRED` | 400 | 邀请码已过期 | 邀请码已过期，请联系家庭主人重新生成 |
| `ALREADY_FAMILY_MEMBER` | 400 | 已是该家庭成员 | 你已是该家庭成员 |
| `OWNER_CANNOT_LEAVE` | 400 | 家庭主人不能退出 | 家庭主人不能退出，请先转让主人身份 |
| `TRANSFER_TARGET_NOT_MEMBER` | 400 | 转让目标不是家庭成员 | 目标用户不是该家庭成员 |
| `CANNOT_REMOVE_OWNER` | 400 | 不能移除家庭主人 | 不能移除家庭主人 |
| `NOT_FAMILY_MEMBER` | 403 | 不是该家庭成员 | 你不是该家庭成员 |

---

## 5. 测试清单

### 5.1 核心测试点

| TC-ID | 类型 | 场景描述 | 覆盖规则ID | 预期结果 |
|-------|------|----------|------------|----------|
| TC-14 | 集成 | 创建家庭，创建者自动成为 OWNER | BR-03 | 201，family + member 记录创建 |
| TC-15 | 集成 | 家庭主人生成邀请码 | BR-05 | 200，返回 8 位邀请码，旧码失效 |
| TC-16 | 单测 | 邀请码字符集校验（不含 0/O/I/1） | BR-05 | 生成的码只包含合法字符 |
| TC-17 | 集成 | 用户通过邀请码加入家庭 | BR-03, BR-04, BR-05 | 200，新增 member 记录 |
| TC-18 | 单测 | 家庭已满 10 人时加入被拒 | BR-04 | 400，`FAMILY_MEMBER_LIMIT_EXCEEDED` |
| TC-19 | 单测 | 用户已是该家庭成员时再次加入 | - | 400，`ALREADY_FAMILY_MEMBER` |
| TC-20 | 集成 | 家庭主人移除成员 | - | 200，member 记录删除 |
| TC-21 | 集成 | 家庭主人转让主人身份 | - | 200，角色互换 |
| TC-22 | 集成 | 家庭成员退出家庭 | - | 200，member 记录删除 |
| TC-23 | 集成 | 解散家庭，所有关联数据物理删除 | BR-17 | 200，family + members + invitations 清除 |
| TC-24 | 单测 | 用户已加入 5 个家庭时创建/加入被拒 | BR-03 | 400，`FAMILY_LIMIT_EXCEEDED` |
| TC-25 | 集成 | 查看家庭内宠物（含多成员的宠物、标注主人昵称） | - | 200，按成员归属返回宠物列表 |
| TC-26 | 单测 | 家庭主人尝试退出被拒 | - | 400，`OWNER_CANNOT_LEAVE` |
| TC-27 | 单测 | 非主人尝试生成邀请码 / 移除成员 / 解散家庭 | - | 403，`FAMILY_ACCESS_DENIED` |
| TC-28 | 集成 | 邀请码过期后使用 | BR-05 | 400，`INVITATION_EXPIRED` |
| TC-29 | 单测 | 邀请码不区分大小写（小写输入转大写匹配） | BR-05 | 正常加入 |

### 5.2 必测边界

- 家庭名称长度边界（1字符/2字符/20字符/21字符）
- 家庭成员数边界（第9人/第10人/第11人）
- 用户家庭数边界（第4个/第5个/第6个）
- 家庭只有主人一个人时解散
- 转让后原主人再执行主人操作（应被拒）
- 被移除的成员用新邀请码重新加入（应允许）
- 同一邀请码两个用户同时使用（并发安全）

### 5.3 质量门禁

- checkstyle：通过
- 全量测试（含 P0）：通过
- 核心验收用例（AC-14 ~ AC-24）：通过

---

## 6. 代码组织（模块结构）

```
soulpal-api/src/main/java/com/soulpal/api/
├── modules/
│   ├── auth/                          # 已有
│   ├── user/                          # P0 已实现
│   └── family/                        # 新建
│       ├── interfaces/
│       │   ├── FamilyController.java
│       │   ├── FamilyMemberController.java
│       │   └── dto/
│       ├── application/
│       │   └── FamilyService.java
│       ├── domain/
│       │   ├── entity/
│       │   │   ├── Family.java
│       │   │   ├── FamilyMember.java
│       │   │   └── FamilyInvitation.java
│       │   └── repository/
│       │       ├── FamilyRepository.java
│       │       ├── FamilyMemberRepository.java
│       │       └── FamilyInvitationRepository.java
│       └── infrastructure/
│           └── persistence/
│               ├── FamilyDO.java
│               ├── FamilyMapper.java
│               ├── FamilyRepositoryImpl.java
│               ├── FamilyMemberDO.java
│               ├── FamilyMemberMapper.java
│               ├── FamilyMemberRepositoryImpl.java
│               ├── FamilyInvitationDO.java
│               ├── FamilyInvitationMapper.java
│               ├── FamilyInvitationRepositoryImpl.java
│               └── converter/
│                   ├── FamilyConverter.java
│                   ├── FamilyMemberConverter.java
│                   └── FamilyInvitationConverter.java
```

### 跨模块依赖

- `FamilyService` → `UserRepository`（查询成员昵称、头像）
- `FamilyService` → `PetRepository`（查询家庭内宠物）
- 依赖方向：family 模块依赖 user 模块的 domain 层接口，不依赖 infrastructure

---

## 7. 快审记录

| 日期 | 结论 | 关键问题 | Owner | 截止时间 |
|------|------|----------|-------|----------|
| - | - | - | - | - |

---

## 8. 变更联动

| 变更ID | 来源 | 变更摘要 | 对 Tech Pack 影响 | 状态 |
|--------|------|----------|-------------------|------|
| CL-02 | P2 → P1 | 创建/加入家庭（API-13, API-19）增加邮箱验证检查 | FamilyService 需增加邮箱验证状态判断 | 待 P2 开发时实施 |
