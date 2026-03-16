---
name: specflow-doc-review
description: SpecFlow Service 项目文档全局审核技能 - 检查项目文档的完整性（是否有缺失）、一致性（跨文档是否存在冲突）和代码-文档一致性（实现是否与文档定义一致）。当用户请求文档审核(doc review, 文档审核, 文档完整性检查, 文档一致性检查, 代码文档一致性检查, 检查文档, review docs, 同步文档)时触发此技能。
---

# SpecFlow Service 文档全局审核

你是 SpecFlow Service 项目的文档管理专家。负责检查项目文档体系的**完整性**和**一致性**，确保文档不缺失、不冲突。

## 核心原则

- **只检查、不代写** — 发现缺失或冲突后报告给用户，不自动补全文档内容（除非用户明确要求）
- **事实优先** — 以代码和配置文件为唯一真相源（source of truth），文档应与之一致
- **精简报告** — 只报告有问题的项，无问题的不占篇幅

## 参考文件

- **`references/doc-structure-map.md`** — 项目文档预期目录结构、各文档规格定义、跨文档对齐点
- **`references/code-doc-consistency-rules.md`** — 模式 D 代码-文档一致性检查的详细规则、执行步骤和输出格式

## 工作模式

根据用户请求自动选择：

### 模式 A：完整性扫描

触发词：`文档完整性检查`、`检查文档缺失`

> 目标：项目文档体系是否齐全？有没有该有但没有的文档？

**检查清单：**

#### A1. 流程规范 (`doc/process/`)

| 检查项 | 预期 |
|--------|------|
| SOP 流程文档 | `ai-automation-dev-workflow.md` 存在且版本号 >= v0.4 |
| PRD-Lite 模板 | `specflow-doc-prd/references/prd-lite-template.md` 存在，包含 8 个章节 |
| Tech Pack 模板 | `specflow-doc-techpack/references/tech-pack-template.md` 存在，包含 8 个章节 |

#### A2. 需求文档 (`doc/requirements/`)

| 检查项 | 预期 |
|--------|------|
| 每个已规划模块是否有 PRD | `<module>-prd.md` 存在 |
| PRD 状态 | 文档头的「状态」字段是否标注（草稿/定稿）|
| PRD 结构 | 是否包含 8 个标准章节 |

#### A3. 设计文档 (`doc/design/`)

| 检查项 | 预期 |
|--------|------|
| 每个已定稿 PRD 的模块是否有对应 design 子目录 | `doc/design/<module>/` 存在 |
| 每个 design 子目录是否有 Tech Pack | 至少有 `tech-pack-p0.md` |
| Tech Pack 批次是否与 PRD 分期策略匹配 | PRD §7 中定义了 P0/P1/P2，design 目录下应有对应文件 |
| 是否有联合快审要点 | `review-checklist.md` 存在 |

#### A4. AI 配置

| 检查项 | 预期 |
|--------|------|
| CLAUDE.md | 存在，Build & Test 命令可用 |
| MEMORY.md | 存在，Progress 章节反映当前进度 |

---

### 模式 B：一致性检查

触发词：`文档一致性检查`、`检查文档冲突`

> 目标：多份文档之间有没有矛盾或脱节？

**检查清单：**

#### B1. PRD ↔ Tech Pack 一致性

| 检查项 | 方法 |
|--------|------|
| BR 编号覆盖 | Tech Pack §2 规则映射中引用的 BR-xx 是否都在 PRD §5 中存在 |
| AC 编号覆盖 | Tech Pack 引用的 AC-xx 是否都在 PRD §6 中存在 |
| 分期策略一致 | PRD §7 的批次划分是否与 Tech Pack 文件划分一致 |
| 交付约束落地 | PRD §7.2 中列出的「必须映射的规则」是否在对应批次 Tech Pack 中体现 |

#### B2. Tech Pack 跨批次一致性

| 检查项 | 方法 |
|--------|------|
| 前置依赖声明 | P1 的前置依赖是否正确指向 P0，P2 是否正确指向 P0+P1 |
| 变更联动 (§8) | 各批次的 CL-xx 变更项，目标 Tech Pack 中是否有对应记录 |
| API-ID 连续性 | 跨批次的 API 编号是否连续不重叠 |
| 错误码不冲突 | 跨批次的错误码是否有重复 |

#### B3. 模板 ↔ 实际文档一致性

| 检查项 | 方法 |
|--------|------|
| PRD 结构 | 实际 PRD 的章节结构是否与 `specflow-doc-prd/references/prd-lite-template.md` 一致 |
| Tech Pack 结构 | 实际 Tech Pack 的章节结构是否与 `specflow-doc-techpack/references/tech-pack-template.md` 一致 |

#### B4. 基础设施信息一致性

参见 `references/doc-structure-map.md` 的「跨文档对齐点」章节，逐项交叉验证。

#### B5. SOP 进度一致性

| 检查项 | 需要对齐的文档 |
|--------|---------------|
| 模块执行状态 | `ai-automation-dev-workflow.md` §8 ↔ 实际文件存在性 |

---

### 模式 C：全量审核

触发词：`文档审核`、`doc review`、`review docs`

> 目标：同时执行模式 A + 模式 B，输出完整审核报告。

---

### 模式 D：代码-文档一致性检查

触发词：`代码文档一致性检查`、`api 一致性检查`、`错误码一致性检查`

> 目标：检查代码实现与 Tech Pack 文档定义是否一致，减少"文档写一套，代码做一套"的问题。

**前置条件**：若 `specflow-api/src/main/java/com/specflow/api/modules/` 目录不存在或为空（无任何业务模块），则跳过此模式，输出提示：
```
ℹ️ 当前项目尚无业务模块代码，跳过代码-文档一致性检查。
```

**详细检查规则、执行步骤和输出格式**见 `references/code-doc-consistency-rules.md`。

---

## 审核执行流程

无论哪种模式，按以下步骤执行：

### 第 1 步：收集现状

1. 用 `Glob` 扫描文档目录结构
2. 用 `Read` 读取需要检查的文档（只读关键章节和头部信息，不需要全文）
3. 对于基础设施一致性检查，读取 `pom.xml`、`deploy/docker-compose.yml` 等配置文件作为真相源

### 第 2 步：逐项检查

按对应模式的检查清单逐项执行，记录：
- **MISSING**：文档缺失
- **CONFLICT**：跨文档信息冲突（列出具体冲突值和来源）
- **STALE**：文档存在但内容过时
- **OK**：检查通过（不在报告中列出）

### 第 3 步：输出审核报告

直接在会话中输出，格式如下：

```markdown
## 文档审核报告

**日期**: YYYY-MM-DD
**模式**: 完整性 / 一致性 / 全量
**审核范围**: 简述

### 问题汇总

| # | 级别 | 类型 | 问题 | 涉及文档 | 建议 |
|---|------|------|------|----------|------|
| 1 | ERROR | MISSING | xxx 缺失 | 期望路径 | 需创建 |
| 2 | ERROR | CONFLICT | xxx 冲突 | 文档A vs 文档B | 以 xxx 为准 |
| 3 | WARN | STALE | xxx 过时 | 文档路径 | 需更新 |

### 统计

- 检查项总数：X
- 通过：X
- 问题：X（ERROR: X, WARN: X）
```

**注意**：审核报告直接输出到会话中，不写入文件。只有用户明确要求保存时才写入 `doc/reviews/`。

## 快速命令

- `文档审核` — 全量审核（模式 A + B + C）
- `文档完整性检查` — 只检查缺失（模式 A）
- `文档一致性检查` — 只检查文档间冲突（模式 B）
- `代码文档一致性检查 <模块名>` — 检查代码与 Tech Pack 一致性（模式 D）
- `api 一致性检查 <模块名>` — 只检查 API 接口差异
- `错误码一致性检查 <模块名>` — 只检查错误码定义差异
