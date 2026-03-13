# 文档目录

本目录存放项目开发过程中的需求文档、技术设计文档、流程方法论与审核报告。

## 目录结构

```
doc/
├── README.md              # 本文件
├── design/                # 设计文档
├── process/               # 流程方法论与模板
├── requirements/          # 需求文档
└── reviews/               # 文档审核报告
```

## 文档说明

### design/ - 设计文档

存放开发过程中的技术设计文档，如数据库设计、接口设计、技术方案等。

### process/ - 流程方法论与模板

存放团队协作流程、SOP、文档模板等方法论资产。

**当前内容包括**:
- AI 自动化开发流程（小团队极简版）
- PRD-Lite 模板
- Tech Pack 模板

### requirements/ - 需求文档

存放需求功能说明、业务规则定义等产品需求相关文档。

### reviews/ - 文档审核报告

每次完成重大阶段后，通过 `/soulpal-doc-review` 技能生成的系统性文档审核报告。

**内容包括**:
- 本次工作摘要
- 文档更新清单
- 一致性检查结果
- 新增文档详情
- 经验教训汇总
- 建议与下一步工作

**命名规范**: `YYYY-MM-DD-<阶段名>-doc-review.md`

**使用场景**:
- 完成一个 SOP 阶段后
- 重大部署操作后
- 架构或技术选型变更后
- 用户主动请求文档审核时

---

## 相关文档

- 核心计划文档: [soulpal_plan/](../soulpal_plan/)
- 部署实录: [deploy/](../deploy/)
- 架构设计: [soulpal_plan/工程架构方案定稿（DDD light）.md](../soulpal_plan/工程架构方案定稿（DDD%20light）.md)
- 项目进度: [soulpal_plan/项目启动记录.md](../soulpal_plan/项目启动记录.md)
