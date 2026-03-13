# AgentTeams 探索指引

> 状态：`观察中，未纳入正式流程`
> 更新时间：2026-02-10
> 前置条件：主流程（ai-automation-dev-workflow.md）经过至少 1 个模块的真实验证后，再启动试验
> 官方状态：实验性功能（experimental），默认关闭

---

## 1. 为什么单独写这份文档

AgentTeams 和我们的开发流程都处于早期：

- 流程本身还没经过真实项目打磨，规则和边界尚未固化。
- AgentTeams 是实验阶段功能，有已知限制（不支持 session 恢复、任务状态可能滞后等）。
- 多 agent 协同的核心前提是**边界清晰**——任务拆分不清晰时，协调成本会远大于并行收益。

原则：**先让单 agent 流程跑通，再考虑多 agent 并行。**

---

## 2. AgentTeams 是什么（核心理解）

- 一个 lead session 协调多个 teammate session，每个 teammate 有独立上下文。
- teammate 之间可以直接通信、互相挑战，不必经过 lead 中转。
- 共享任务列表，支持任务依赖和自动认领。
- 与 subagent 的区别：subagent 只能向主 agent 汇报；teammate 之间可以对话协作。

**关键约束：**

- token 消耗随 teammate 数量线性增长。
- 两个 teammate 不能编辑同一个文件（会互相覆盖）。
- 不支持嵌套（teammate 不能再建团队）。
- 一个 session 同时只能管理一个 team。

---

## 3. 潜在嵌入点（待验证）

以下是理论上可能有价值的嵌入点，**需要在流程稳定后逐一试验**。

### 3.1 阶段 B — Tech Pack 多角度审查

**假设**：快审前用 3 个 teammate 从不同角度（规则完整性 / 安全 / 数据一致性）并行审查 Tech Pack，互相挑战结论，可以提升快审会议效率。

**验证条件**：

- [ ] 主流程已跑通至少 1 个完整模块
- [ ] 有一份真实的 Tech Pack 可供审查
- [ ] 对照：同一份 Tech Pack，单 agent 审查 vs AgentTeams 审查，比较发现问题的数量和质量

**风险**：审查类任务可能用 subagent 就够了（不需要 teammate 之间互相对话），agentTeams 的额外 token 成本未必值得。

### 3.2 阶段 D — 跨层并行开发

**假设**：利用 DDD Light 分层（infrastructure / domain / application / interfaces）天然的文件隔离，每层分配一个 teammate 并行开发，最后一个 teammate 负责测试。

**验证条件**：

- [ ] 主流程已跑通至少 2 个完整模块，分层边界已经过实战验证
- [ ] 选一个新模块（≥ 5 个接口）试验，和纯单 agent 开发做对比
- [ ] 记录：总耗时、token 消耗、文件冲突次数、返工次数

**风险**：层与层之间有依赖（domain 定义接口，infrastructure 实现），如果任务依赖设置不当，teammate 会空等或基于错误假设开发。

---

## 4. 不适用的场景

| 场景 | 原因 |
|------|------|
| 业务决策（阶段 A） | 必须由人做，多 agent 无法替代 |
| 单文件修改 / bug 修复 | 无法并行，协调开销 > 收益 |
| 小模块（< 3 个接口） | 拆不出有意义的并行单元 |
| 顺序依赖强的任务 | AgentTeams 的优势在并行，串行任务用单 session 更高效 |

---

## 5. 试验计划

### 第一轮（流程稳定后）

- 目标：验证阶段 B（Tech Pack 审查）的可行性
- 方法：用一个已完成的模块的 Tech Pack，分别用单 agent 和 AgentTeams 审查，对比结果
- 记录：发现问题数量、质量、token 消耗、总耗时

### 第二轮（第一轮有效后）

- 目标：验证阶段 D（跨层并行开发）的可行性
- 方法：选一个新模块，用 AgentTeams 开发，同时用单 agent 开发另一个类似规模模块作为对照
- 记录：开发时长、返工次数、文件冲突、token 消耗、代码质量

### 判定标准

- 如果 AgentTeams 在两轮试验中都没有显著优于单 agent，则暂时搁置，回归单 agent 流程。
- 如果某个嵌入点验证有效，再将其写入 workflow 的对应阶段。

---

## 6. 启用方法（备忘）

```json
// ~/.claude/settings.json
{
  "env": {
    "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1"
  }
}
```

官方文档：https://code.claude.com/docs/en/agent-teams
