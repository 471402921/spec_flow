# 流程方法论目录

本目录用于集中管理团队流程方法论与模板文档，供持续讨论和迭代。

## Quick Start（新模块开发 checklist）

1. [ ] 基于 `prd-lite-template.md` 输出 PRD-Lite，产品确认后冻结版本
2. [ ] AI 生成 Tech Pack 初稿，后端主责审核约束和决策项
3. [ ] 组织 90 分钟快审，结论：通过 / 退回
4. [ ] 通过后进入 AI 主开发，人工按 checklist 审查关键点
5. [ ] 上线门禁全部通过后发布

## 文件说明

| 文件 | 用途 | 谁来填 |
|------|------|--------|
| `ai-automation-dev-workflow.md` | 端到端流程与原则 | 团队共识 |
| `prd-lite-template.md` | PRD-Lite 模板 | 产品&UI&UE |
| `tech-pack-template.md` | Tech Pack 模板（约束与决策） | 后端主责 |
| `prd-change-log-template.md` | PRD 变更记录 | 变更发起人 |
| `agent-teams-exploration.md` | AgentTeams 探索指引（未纳入正式流程） | 后端主责 |

## 使用建议

1. 新模块先基于 `prd-lite-template.md` 输出 PRD-Lite。
2. 再基于 `tech-pack-template.md` 生成技术规格并组织快审。
3. 每轮流程调整，优先更新本目录文档并记录版本。
