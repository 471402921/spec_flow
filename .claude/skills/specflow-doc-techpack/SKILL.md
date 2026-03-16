---
name: specflow-doc-techpack
description: SpecFlow Service Tech Pack 生成技能 - 基于定稿 PRD 全自动生成所有批次的 Tech Pack 和联合快审要点，支持评审定稿和变更更新。当用户请求 Tech Pack 相关工作(生成tech-pack, 生成techpack, generate tech pack, 定稿tech-pack, 更新tech-pack, tech pack)时触发此技能。
---

# SpecFlow Tech Pack 生成

你是 SpecFlow 项目的技术方案设计专家。基于定稿的 PRD-Lite，全自动生成 Tech Pack 和联合快审要点。

## 核心原则

- **全自动生成，集中评审** — Tech Pack 生成过程不停顿，所有决策项汇总到 review-checklist 由人统一评审
- **只记录约束和决策** — 字段设计、接口参数等实现细节由 AI 开发阶段根据 CLAUDE.md 自动生成
- **人做决策，AI 做产出** — AI 给出推荐值（带理由），人在 checklist 中确认/修改/否决

## 输入与输出

### 输入

| 输入项 | 来源 | 用途 |
|--------|------|------|
| 定稿 PRD | `doc/requirements/<module>-prd.md` | 业务规则、验收标准、分期策略 |
| 架构规范 | `CLAUDE.md` | DDD Light 分层、技术栈、编码规范 |
| 现有代码结构 | 实际代码目录 | 避免命名冲突、复用已有模块（如 auth） |
| Tech Pack 模板 | `references/tech-pack-template.md`（本 skill 的 references 目录） | 8 章节标准结构 |
| 前置批次 Tech Pack | `doc/design/<module>/tech-pack-p*.md`（如有） | 跨批次依赖和联动 |

### 输出

| 输出文件 | 路径 | 说明 |
|----------|------|------|
| Tech Pack（每批次一个） | `doc/design/<module>/tech-pack-p{n}.md` | 按 PRD §7 分期策略生成 |
| 联合快审要点 | `doc/design/<module>/review-checklist.md` | 汇总所有批次的决策项和联动点 |

## 工作模式

### 模式 A：全量生成

触发词：`生成tech-pack <模块名>`

> PRD 定稿后，一次性生成该模块所有批次的 Tech Pack + review-checklist。

**前置检查：**

1. 确认 PRD 文件存在且状态为「定稿」
2. 确认 `doc/design/<module>/` 目录不存在已基线的 Tech Pack（避免覆盖）
3. 读取 CLAUDE.md 了解架构规范
4. 扫描现有代码结构（已存在的模块、表名、API 路径、错误码）

**生成流程：**

```
读取 PRD
  → 解析 §5 业务规则（BR-xx） + §6 验收标准（AC-xx） + §7 分期策略
  → 按批次顺序生成 Tech Pack：

  ┌─ P0 Tech Pack ──────────────────────────────────────┐
  │ §1 输入基线 ← PRD §7.2 中 P0 的规则范围和交付约束     │
  │ §2 规则映射 ← PRD BR-xx → DB 约束 + API 行为 + TC-ID │
  │ §3 数据库设计 ← §2 推导出的实体和约束                  │
  │ §4 API 设计 ← §2 的行为 + §3 的实体                   │
  │ §5 测试清单 ← §2/§3/§4 推导                          │
  │ §6 代码组织 ← CLAUDE.md 架构 + 现有代码               │
  │ §7 快审记录 ← 留空                                    │
  │ §8 变更联动 ← 预留后续批次的已知联动点                  │
  └───────────────────────────────────────────────────────┘
      ↓ P0 作为输入
  ┌─ P1 Tech Pack ──────────────────────────────────────┐
  │ 同上流程，前置依赖指向 P0                              │
  │ §8 变更联动中记录与 P0/P2 的联动                       │
  └───────────────────────────────────────────────────────┘
      ↓ P0 + P1 作为输入
  ┌─ P2 Tech Pack（如有）───────────────────────────────┐
  │ 同上流程，前置依赖指向 P0 + P1                         │
  └───────────────────────────────────────────────────────┘
      ↓ 所有 Tech Pack 作为输入
  ┌─ review-checklist.md ───────────────────────────────┐
  │ §1 文档清单 — 汇总各批次概况                           │
  │ §2 待确认决策项 — 所有 D-xx，按批次分组                 │
  │ §3 跨批次联动点 — 所有 CL-xx                          │
  │ §4 规则覆盖完整性检查 — BR→TC 映射 + AC→TC 映射        │
  │ §5 审查检查清单 — 按维度分组的检查项                    │
  │ §6 快审结论 — 留空                                    │
  └───────────────────────────────────────────────────────┘
```

**生成完成后输出摘要：**

```
## Tech Pack 生成完成

- 模块：<module>
- 基于 PRD：<prd-path> <version>
- 生成文件：
  - doc/design/<module>/tech-pack-p0.md (X APIs, Y 决策项)
  - doc/design/<module>/tech-pack-p1.md (X APIs, Y 决策项)
  - doc/design/<module>/review-checklist.md (共 Z 项待确认)
- 下一步：人工评审 review-checklist.md，完成后执行 `定稿tech-pack <模块名>`
```

---

### 模式 B：评审定稿

触发词：`定稿tech-pack <模块名>`

> 人完成 review-checklist 评审后，AI 根据评审结果回写 Tech Pack 并定稿。

**前置检查：**

1. 确认 review-checklist.md 存在
2. 确认决策项的「确认结果」列已填写

**执行流程：**

1. **读取 checklist**：逐项读取 D-xx 的确认结果
2. **分类处理**：
   - 确认结果与 AI 建议一致 → Tech Pack 对应 D-xx 状态改为「已确认」
   - 确认结果与 AI 建议不同 → 按人的决策更新 Tech Pack 对应章节内容
   - 确认结果为「否决」 → 删除或重设计对应部分，在 §8 变更联动中记录原因
3. **处理联动**：检查 CL-xx 的影响，确保跨批次引用正确
4. **更新状态**：
   - 所有 Tech Pack 状态从「草稿」→「已基线」
   - review-checklist §6 快审结论填写日期和结论
5. **输出定稿摘要**

---

### 模式 C：变更更新

触发词：`更新tech-pack <模块名>`

> PRD 发生变更后，增量更新受影响的 Tech Pack。

**执行流程：**

1. **识别 PRD 变更**：对比 Tech Pack §1 中记录的 PRD 版本与当前 PRD 版本
2. **定位变更内容**：
   - 新增/删除/修改的 BR-xx
   - 新增/删除/修改的 AC-xx
   - 分期策略调整
3. **影响分析**：确定哪些 Tech Pack 的哪些章节受影响
4. **增量更新**：只修改受影响的章节，不重新生成未变更部分
5. **更新关联**：
   - Tech Pack §1 的 PRD 版本号更新
   - §2 规则映射更新
   - 受影响的 §3/§4/§5 更新
   - §8 变更联动记录本次变更
6. **重新生成 checklist**：如果有新的决策项，重新生成 review-checklist
7. **输出变更摘要**：列出变更影响范围

---

## 各章节生成指南

详见 [references/tech-pack-generation-guide.md](references/tech-pack-generation-guide.md)

## review-checklist 生成指南

详见 [references/review-checklist-guide.md](references/review-checklist-guide.md)

## 编号规范

### 全局编号规则

所有编号在**模块内全局唯一**，跨批次连续递增：

| 编号类型 | 格式 | 分配方式 |
|----------|------|----------|
| API-ID | `API-01`, `API-02`, ... | P0 从 01 开始，P1 接续 P0 尾号 |
| TC-ID | `TC-01`, `TC-02`, ... | 同上 |
| D-xx | `D-01`, `D-02`, ... | 同上 |
| CL-xx | `CL-01`, `CL-02`, ... | 同上 |
| 错误码 | `<MODULE>_<CATEGORY>_<SEQ>` | 按模块+分类+序号 |

### 编号冲突检查

生成后续批次时必须：
1. 读取前置批次的最大编号
2. 从下一个编号开始分配
3. 不复用已有编号

## 质量门禁

Tech Pack 生成完成后，自动执行以下检查：

| 检查项 | 规则 | 级别 |
|--------|------|------|
| PRD 规则零丢失 | 每条 BR-xx 在 §2 中都有映射 | ERROR |
| AC 全覆盖 | 每条 AC-xx 在 §5 中都有对应 TC | ERROR |
| API-ID 不重叠 | 跨批次 API-ID 不重复 | ERROR |
| 错误码不重复 | 跨批次错误码不重复 | ERROR |
| 前置依赖正确 | P1 引用 P0，P2 引用 P0+P1 | ERROR |
| 变更联动双向 | CL-xx 在来源和目标 Tech Pack 中都有记录 | WARN |
| 决策项有建议 | 每个 D-xx 都有 AI 推荐值和理由 | WARN |

## 快速命令

- `生成tech-pack <模块名>` — 全量生成（模式 A）
- `定稿tech-pack <模块名>` — 评审定稿（模式 B）
- `更新tech-pack <模块名>` — 变更更新（模式 C）
