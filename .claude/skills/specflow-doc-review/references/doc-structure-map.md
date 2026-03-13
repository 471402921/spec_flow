# SpecFlow Service 文档结构地图

## 预期目录结构

```
doc/
├── process/                          # 流程规范（模板 + SOP）
│   ├── ai-automation-dev-workflow.md  # 四阶段 SOP
│   ├── prd-lite-template.md           # PRD-Lite 模板
│   ├── tech-pack-template.md          # Tech Pack 模板
│   └── prd-change-log-template.md     # PRD 变更日志模板
├── requirements/                      # 需求文档
│   └── <module>-prd.md                # 各模块 PRD（如 user-module-prd.md）
├── design/                            # 设计文档（按模块分子目录）
│   └── <module>/
│       ├── tech-pack-p0.md            # P0 批次 Tech Pack
│       ├── tech-pack-p1.md            # P1 批次 Tech Pack
│       ├── tech-pack-p2.md            # P2 批次 Tech Pack（如有）
│       └── review-checklist.md        # 联合快审要点
├── reviews/                           # 审核记录
│   └── YYYY-MM-DD-<主题>-doc-review.md
└── README.md                          # 文档索引

specflow_plan/                          # 基础设施与架构文档
├── 项目启动记录.md                     # SOP 阶段进度
├── 部署架构设计（Cloudflare Tunnel + 家庭节点）.md
├── 工程架构方案定稿（DDD light）.md
├── ADR-0001 技术选型定稿 v2.0.md
├── 项目背景介绍.md
└── MCP 清单（工程建设与运维阶段可用）.md

CLAUDE.md                             # 项目级 AI 指令
.claude/memory/MEMORY.md              # AI 记忆（进度 + 经验教训）
```

## 各文档预期规格

### 流程规范 (`doc/process/`)

| 文档 | 必须存在 | 预期内容 |
|------|---------|---------|
| `ai-automation-dev-workflow.md` | 是 | 四阶段 SOP（A→B→C→D），版本号 >= v0.4 |
| `prd-lite-template.md` | 是 | 8 个标准章节模板 |
| `tech-pack-template.md` | 是 | 8 个标准章节模板 |
| `prd-change-log-template.md` | 是 | 变更日志格式模板 |

### 需求文档 (`doc/requirements/`)

| 规则 | 说明 |
|------|------|
| 命名 | `<module>-prd.md`（如 `user-module-prd.md`） |
| 状态字段 | 文档头必须标注「状态：草稿/定稿」 |
| 标准章节 | 8 章：概述、角色权限、核心实体、功能需求、业务规则、验收标准、交付策略、未来扩展 |
| 编号体系 | FR-x.x.x（功能需求）, BR-xx（业务规则）, AC-xx（验收标准） |

### 设计文档 (`doc/design/`)

| 规则 | 说明 |
|------|------|
| 目录结构 | 每个已**定稿** PRD 的模块必须有 `doc/design/<module>/` 子目录 |
| Tech Pack | 至少有 `tech-pack-p0.md`，批次数量须与 PRD §7 分期策略匹配 |
| 联合快审 | `review-checklist.md` 必须存在 |
| Tech Pack 章节 | 8 章：输入基线、规则映射、数据库设计、API 设计、测试清单、代码组织、决策项、变更协调 |
| 编号体系 | API-xx, D-xx（决策项）, CL-xx（变更联动） |

### 基础设施文档 (`specflow_plan/`)

| 文档 | 真相源（Source of Truth） |
|------|------------------------|
| 项目启动记录 | 阶段完成状态须与 `MEMORY.md` Progress 章节一致 |
| 部署架构设计 | 须与 `deploy/docker-compose.yml` 和 `deploy/deploy.sh` 配置一致 |
| 技术选型 ADR | Java/Spring Boot 版本须与根 `pom.xml` 一致 |

### AI 配置文件

| 文档 | 预期内容 |
|------|---------|
| `CLAUDE.md` | Build & Test 命令、架构说明、代码质量规范、部署信息 |
| `MEMORY.md` | Progress 章节反映当前阶段进度、Lessons Learned 持续更新 |

## 跨文档对齐点

以下信息散落在多份文档中，审核时需交叉验证：

| 对齐项 | 涉及文档 |
|--------|---------|
| 端口号 | 部署架构设计、`deploy/docker-compose.yml`、CLAUDE.md |
| 项目路径 | 部署架构设计、MEMORY.md、`deploy/deploy.sh` |
| 镜像版本 | 部署架构设计、`deploy/docker-compose.yml` |
| 域名 | 部署架构设计、CLAUDE.md |
| Java/Spring Boot 版本 | ADR 技术选型、`pom.xml`、CLAUDE.md |
| 部署命令 | 部署架构设计、MEMORY.md、CLAUDE.md |
| 阶段完成状态 | `specflow_plan/项目启动记录.md`、MEMORY.md |
| 模块执行状态 | `ai-automation-dev-workflow.md` §8、实际文件存在性 |
