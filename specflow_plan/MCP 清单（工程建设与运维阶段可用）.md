# MCP 清单（工程建设与运维阶段可用）

> 本清单用于 **工程搭建、部署、运维、调试、文档与协作** 阶段，不涉及 AI 提示词设计，仅作为“能力插件目录”。  
> 原则：**少而精、可替换、按阶段启用**。

---

## 1. 基础运维与部署类（必选）

### 1.1 SSH MCP
- **用途**
  - 远程执行命令（部署、回滚、日志查看、备份恢复）
  - 操作 Docker / Docker Compose
- **使用阶段**
  - 全阶段（dev / staging / prod）
- **定位**
  - 应用级运维执行器（当前架构的核心运维工具）
- **标准入口**
  - `ssh home-node`（已配置别名）
- **推荐能力**
  - Key-based 登录
  - 命令白名单 / 超时控制
- **说明**
  - 配合 Cloudflare Tunnel，构成完整的家庭节点部署能力

---

## 2. 容器与运行时相关（可选）

### 2.1 Docker MCP
- **用途**
  - 管理容器、镜像、网络、volume
  - 查询运行状态、资源使用
- **使用阶段**
  - Staging / Production（规模上来后）
- **定位**
  - 运维增强工具
- **v1.0 建议**
  - 非必选
  - SSH 直接操作 Docker 即可满足需求

---

## 3. 数据与存储类（可选）

### 3.1 PostgreSQL MCP
- **用途**
  - 执行只读查询
  - 数据校验、调试
  - 备份结果抽查
- **使用阶段**
  - Staging / Production（受控使用）
- **注意**
  - 严禁在 prod 执行写操作
- **定位**
  - 数据可观测与审计辅助

---

### 3.2 Redis MCP
- **用途**
  - 查看缓存状态
  - 调试限流/会话/状态
- **使用阶段**
  - Dev / Staging
- **定位**
  - 运行时调试工具
- **备注**
  - 生产环境谨慎使用，避免误删 key

---

## 4. 接口与协议类（强烈推荐）

### 4.1 OpenAPI MCP
- **用途**
  - 校验 OpenAPI 文档合法性
  - 从 OpenAPI 生成：
    - Mock
    - 客户端 SDK
    - 接口测试用例
- **使用阶段**
  - 全阶段
- **定位**
  - HTTP 契约的“事实标准执行者”
- **说明**
  - 与 DDD light 架构强绑定
  - 建议 CI 中强制校验

---

### 4.2 AsyncAPI MCP（可选 · 推荐占位）
- **用途**
  - 描述 WebSocket / 消息事件协议
  - 校验事件 schema
- **使用阶段**
  - 当 WS / 事件复杂度上升时
- **定位**
  - 实时与事件契约管理
- **v1.0 策略**
  - 可只做 schema 校验，不做代码生成

---

## 5. 测试与调试类（可选）

### 5.1 HTTP Client MCP
- **用途**
  - 调用 REST API
  - 回归测试 / 联调
- **使用阶段**
  - Dev / Staging
- **定位**
  - 自动化接口测试替代 Postman
- **备注**
  - 与 OpenAPI MCP 配合效果最好

---

### 5.2 WebSocket Client MCP
- **用途**
  - 模拟客户端连接
  - 订阅/接收 WS 事件
- **使用阶段**
  - Dev / Staging
- **定位**
  - 实时链路验证工具

---

## 6. 文档与工程治理类（推荐）

### 6.1 Markdown / Docs MCP
- **用途**
  - 管理 ARCHITECTURE.md / ADR / Runbook
  - 校验 Markdown 结构
- **使用阶段**
  - 全阶段
- **定位**
  - 架构文档治理工具

---

### 6.2 Git MCP
- **用途**
  - 分支管理
  - 提交检查
  - Tag / Release 管理
- **使用阶段**
  - 全阶段
- **定位**
  - 工程版本治理

---

## 7. 监控与日志类（后期引入）

### 7.1 Log Query MCP
- **用途**
  - 查询聚合日志（Loki / Elastic 等）
  - 按 traceId 排障
- **使用阶段**
  - Production（问题排查）
- **定位**
  - 可观测性增强

---

### 7.2 Metrics MCP
- **用途**
  - 查询 Prometheus 指标
  - 资源与性能分析
- **使用阶段**
  - Production（规模上来后）
- **定位**
  - 性能与容量规划

---

## 8. 推荐启用组合（按阶段）

### 8.1 v1.0（现在就用）
- SSH MCP（`ssh home-node`）
- OpenAPI MCP
- Git MCP  

### 8.2 v1.1（业务复杂后）
- AsyncAPI MCP  
- HTTP Client MCP  
- WebSocket Client MCP  

### 8.3 v2.0（规模化后）
- Docker MCP  
- Log Query MCP  
- Metrics MCP  

---

## 9. 总结原则

- **MCP 是能力插件，不是架构核心**
- 能不用就不用，但：
  - 运维自动化（SSH）**必须有**
  - 接口契约（OpenAPI）**必须机器可校验**
- 所有 MCP：
  - 可替换
  - 不侵入 Domain / Application 层
  - 不影响 DDD light 架构纯度

---

> 本清单作为《工程框架搭建方案》的前置参考，用于决定“工具能力边界”，而不是技术实现细节。
