# SpecFlow

**Spec-Driven / Agentic Engineering** — 一套面向小团队的 AI 原生后端开发框架。

SpecFlow 将 **需求驱动的工程流程** 和 **DDD Light 架构脚手架** 整合在一起，通过 Claude Code Skills 实现从 PRD 到部署的全链路 AI 辅助。

## 核心理念

1. **Spec-Driven**: 需求文档（PRD-Lite）→ 技术设计（Tech Pack）→ 实现 → 审核，每一步都有结构化模板
2. **Agentic Engineering**: Claude Code 自定义技能覆盖开发全周期 — 模块生成、代码审核、测试生成、文档审核、部署运维
3. **DDD Light**: 领域驱动设计的务实版本 — 四层模块架构（interfaces / application / domain / infrastructure），domain 层框架无关

## 技术栈

- Java 21 + Spring Boot 3.4.2
- MyBatis-Plus + PostgreSQL + Flyway
- Maven 多模块（api / worker / common）
- Docker Compose 部署
- GitHub Actions CI

## 快速开始

```bash
# 克隆项目
git clone <repo-url> && cd specflow

# 构建
./mvnw clean package -DskipTests

# 运行测试（H2 内存数据库，无需外部依赖）
./mvnw clean test

# 本地启动（需要 PostgreSQL）
./mvnw spring-boot:run -pl specflow-api
```

## AI 开发工作流

安装 [Claude Code](https://claude.ai/code) 后，使用内置技能：

| 技能 | 用途 |
|------|------|
| `/specflow-doc-prd` | 编写/审核 PRD-Lite |
| `/specflow-doc-techpack` | 从 PRD 生成 Tech Pack |
| `/specflow-module-gen` | 根据 Tech Pack 生成模块脚手架 |
| `/specflow-test-gen` | 自动生成单元+集成测试 |
| `/specflow-code-review` | 架构合规性审核 |
| `/specflow-doc-review` | 跨文档一致性检查 |
| `/specflow-deploy` | 部署/回滚/日志/状态 |

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

- [开发流程](doc/process/ai-automation-dev-workflow.md)
- [PRD 模板](doc/process/prd-lite-template.md)
- [Tech Pack 模板](doc/process/tech-pack-template.md)
- [运维手册](deploy/RUNBOOK.md)

## License

Apache 2.0
