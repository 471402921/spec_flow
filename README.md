# SpecFlow

**Spec-Driven / Agentic Engineering** — 一套面向小团队的 AI 原生后端开发框架。

> Spec-driven 的前提不是把模糊需求交给 AI，而是先把关键业务决策结构化，再让 AI 放大执行效率。

## 这是什么，不是什么

SpecFlow 不是"帮不懂业务的人自动做软件"的工具，而是"帮对业务已经有一定清晰度的人，把决策更稳定地传递给模型和团队"的框架。

它的本质是**放大器，不是替代器**。业务理解清晰的人用它会很快；业务理解混乱的人用它，只会更快地产出混乱。

很多 AI 工具失败，不是因为模型不够强，而是因为它们暗示了一种幻觉——好像需求模糊也能被自动整理成靠谱系统。SpecFlow 不做这个承诺。

## 对你的要求

使用这套框架，优先需要的不是编程能力，而是**表达和决策能力**：

**说清楚业务边界**
知道要解决什么、不解决什么，角色是谁、规则是什么、例外怎么处理、优先级如何排。

**做出关键取舍**
很多地方模型无法替你决定：规则冲突怎么裁、第一版范围怎么收、哪些复杂情况先不做。这些决策必须在进入 AI 之前由人完成。

**识别 AI 产出的偏差**
能看出哪里是"合理补全"，哪里已经"擅自改义"。AI 会在你没注意到的地方做假设——你需要有能力发现并纠正它。

## 适合谁

**适合：**

- 对业务有主导权的小团队负责人
- 能写清楚规则的产品经理 / 业务分析师
- 既懂业务又带开发的技术负责人
- 想把 AI 纳入研发流程，但不想失控的团队

**不太适合：**

- 需求长期口头化、没有人愿意写清楚的团队
- 决策权分散、没人拍板范围和规则的团队
- 期望"把需求一丢，AI 自动出完整系统"的用户

## 核心设计

**Spec-Driven 工作流**

需求文档（PRD-Lite）→ 技术设计（Tech Pack）→ 实现 → 审核，每一步都有结构化模板和 AI 辅助。每个阶段有明确的前置条件：没有定稿的 PRD 不会进入 Tech Pack，没有定稿的 Tech Pack 不会进入代码生成。这是刻意设计，不是限制——它强制在正确的时机把业务决策写清楚。

**Agentic Engineering**

Claude Code 自定义技能覆盖开发全周期——模块生成、代码审核、测试生成、文档审核、部署运维。AI 在清晰的结构化上下文中执行，而不是从模糊描述里猜测意图。

**DDD Light 架构**

领域驱动设计的务实版本——四层模块架构（interfaces / application / domain / infrastructure），domain 层框架无关。AI 生成的代码有明确的落点规则，不会因为"随便写"而变成难以维护的大泥球。

## 技术栈

- Java 21 + Spring Boot 3.4.2
- MyBatis-Plus + PostgreSQL + Flyway
- Maven 多模块（api / worker / common）
- Docker Compose 部署
- GitHub Actions CI

## 快速开始

```bash
# 克隆项目
git clone https://github.com/471402921/spec_flow.git && cd spec_flow

# 构建
./mvnw clean package -DskipTests

# 运行测试（H2 内存数据库，无需外部依赖）
./mvnw clean test

# 本地启动（需要 PostgreSQL）
./mvnw spring-boot:run -pl specflow-api
```

## AI 开发工作流

安装 [Claude Code](https://claude.ai/code) 后，通过内置技能完成从需求到部署的完整链路：

| 技能 | 阶段 | 用途 |
|------|------|------|
| `/specflow-doc-prd` | 需求 | 编写/审核 PRD-Lite |
| `/specflow-doc-techpack` | 设计 | 从 PRD 生成 Tech Pack |
| `/specflow-module-gen` | 实现 | 根据 Tech Pack 生成模块脚手架 |
| `/specflow-test-gen` | 测试 | 自动生成单元+集成测试 |
| `/specflow-code-review` | 审核 | 架构合规性审核 |
| `/specflow-doc-review` | 审核 | 跨文档一致性检查 |
| `/specflow-deploy` | 部署 | 部署/回滚/日志/状态 |

## 项目结构

```
specflow-api/          # REST 服务主模块
specflow-worker/       # 异步任务模块（占位）
specflow-common/       # 共享代码（Result<T>、异常体系）
deploy/                # Docker Compose + 部署脚本 + 运维手册
doc/                   # 需求文档、设计文档、流程模板、审核报告
.claude/skills/        # Claude Code 自定义技能定义
```

## 文档

- [开发流程](doc/README.md)
- [运维手册](deploy/RUNBOOK.md)

## License

Apache 2.0
