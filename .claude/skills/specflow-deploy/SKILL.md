---
name: specflow-deploy
description: SpecFlow Service 部署操作技能 - 封装从本地构建到远程部署的完整流程，包括部署前检查、执行部署、健康验证、日志查看、回滚和数据库操作。当用户请求部署相关操作(部署, deploy, 发布, 上线, 回滚, rollback, 查看日志, logs, 重启, restart, 数据库备份, db backup, 健康检查, health check, 服务状态, status)时触发此技能。
---

# SpecFlow Service 部署操作

你是 SpecFlow Service 项目的部署运维助手。负责执行部署、验证健康状态、排查问题和执行回滚。

## 环境配置

**首次使用前**，用户需要配置 `.claude/specflow-env.md`（从 `.claude/specflow-env.example.md` 复制）。

执行任何部署操作前，先读取 `.claude/specflow-env.md`，提取以下配置：
- `deploy.ssh_alias` — SSH 连接别名（如 `my-server`）
- `deploy.service_path` — 服务器上的项目根目录（如 `/srv/specflow-service`）
- `deploy.api_port` — API 服务端口（如 `8080`）
- `database.name` — 数据库名
- `database.user` — 数据库用户名

如果配置文件不存在，提示用户：
```
⛔ 未找到 .claude/specflow-env.md。
请先复制配置模板并填入你的部署环境信息：
  cp .claude/specflow-env.example.md .claude/specflow-env.md
```

以下文档中使用 `{SSH_ALIAS}`、`{SERVICE_PATH}`、`{API_PORT}`、`{DB_NAME}`、`{DB_USER}` 作为占位符，执行时替换为配置文件中的实际值。

## 核心原则

- **确认优先** — 任何影响线上服务的操作（部署、回滚、数据库操作）必须先展示计划，等用户确认后再执行
- **验证闭环** — 每次部署后必须完成健康检查，不能"部署完就走"
- **可追溯** — 操作前记录当前 commit hash，操作后输出完整状态报告
- **最小风险** — 优先使用安全操作，避免 `--force`、`--hard` 等破坏性命令

## 参考文件

- **`references/infra-map.md`** — 基础设施拓扑、连接方式、容器配置、关键路径
- **`references/troubleshooting.md`** — 常见故障排查手册、日志查看命令、回滚流程

## 工作模式

根据用户请求自动选择：

---

### 模式 A：完整部署

触发词：`部署`, `deploy`, `发布`, `上线`

> 从本地代码推送到远程部署并验证的完整流程。

#### A1. 部署前检查（本地）

在用户 Mac 上执行：

```bash
# 1. 确认当前分支和状态
git status
git log --oneline -3

# 2. 确认没有未提交的变更
```

**有未提交的变更 → 提醒用户先提交**。

> 注意：本地未配置 Java 环境，跳过 checkstyle 和测试，构建验证在服务器 Docker 中完成。

#### A2. 推送代码

```bash
git push origin main
```

#### A3. 远程部署

```bash
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && bash deploy.sh"
```

`deploy.sh` 会自动执行：git pull → docker compose down → docker compose up --build → 健康检查

#### A4. 部署验证

```bash
# 容器状态
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && docker compose ps"

# 健康检查
ssh {SSH_ALIAS} "curl -s http://localhost:{API_PORT}/actuator/health"
```

#### A5. 部署报告

```markdown
## 部署报告

**时间**: YYYY-MM-DD HH:MM
**Commit**: {hash} - {message}
**结果**: ✅ 成功 / ❌ 失败

### 容器状态
| 容器 | 状态 | 端口 |
|------|------|------|
| postgres | healthy | 5432 |
| redis | healthy | 6379 |
| api | running | {API_PORT} |

### 健康检查
- 健康检查: ✅ `{"status":"UP"}`
```

---

### 模式 B：服务状态查看

触发词：`服务状态`, `status`, `健康检查`, `health check`

> 快速查看当前部署状态，不做任何修改。

```bash
# 容器状态
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && docker compose ps"

# 当前部署版本
ssh {SSH_ALIAS} "cd {SERVICE_PATH} && git log --oneline -1"

# 健康检查
ssh {SSH_ALIAS} "curl -s http://localhost:{API_PORT}/actuator/health"

# Nginx 状态
ssh {SSH_ALIAS} "sudo systemctl status nginx --no-pager -l"
```

---

### 模式 C：日志查看

触发词：`查看日志`, `logs`, `日志`

> 查看容器日志，支持按容器、时间范围、关键词过滤。

```bash
# API 日志（最近 100 行）
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && docker compose logs --tail=100 api"

# 实时跟踪 API 日志
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && docker compose logs -f --tail=50 api"

# 查看错误日志
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && docker compose logs api | grep -i error"

# 按 TraceId 查询
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && docker compose logs api | grep '{traceId}'"

# Postgres 日志
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && docker compose logs --tail=50 postgres"

# Nginx 日志
ssh {SSH_ALIAS} "sudo tail -50 /var/log/nginx/error.log"
```

根据用户需求选择合适的命令组合。

---

### 模式 D：重启服务

触发词：`重启`, `restart`

> 重启指定容器或全部服务。

```bash
# 仅重启 API（不重建镜像）
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && docker compose restart api"

# 重建并重启 API
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && docker compose up -d --build api"

# 重启全部服务
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && docker compose restart"
```

重启后必须执行健康检查验证。

---

### 模式 E：回滚

触发词：`回滚`, `rollback`

> 将线上代码回退到指定版本。**高风险操作，必须用户确认。**

#### E1. 展示可回滚版本

```bash
ssh {SSH_ALIAS} "cd {SERVICE_PATH} && git log --oneline -10"
```

#### E2. 用户确认目标版本后执行

```bash
ssh {SSH_ALIAS} "cd {SERVICE_PATH} && \
  git checkout {commit-hash} && \
  cd deploy && docker compose up -d --build"
```

> **注意**：回滚不处理数据库迁移。如果回滚涉及 Flyway 迁移变更，需要在 E3 中手动处理。

#### E3. 数据库回滚（如需要）

仅在回滚跨越了 Flyway 迁移版本时需要：

```bash
# 检查当前迁移状态
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && \
  docker compose exec -T postgres psql -U {DB_USER} -d {DB_NAME} \
  -c \"SELECT version, description, installed_on FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;\""
```

Flyway 不支持自动回滚，需手动编写 undo SQL。**展示方案后必须等用户确认再执行。**

#### E4. 回滚验证

同模式 A 的 A4 验证步骤。

---

### 模式 F：数据库操作

触发词：`数据库备份`, `db backup`, `数据库恢复`, `db restore`, `迁移状态`, `migration status`

> 数据库备份、恢复和迁移状态查看。

#### F1. 备份

```bash
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && \
  docker compose exec -T postgres pg_dump -U {DB_USER} {DB_NAME} > backup_$(date +%Y%m%d_%H%M%S).sql"
```

#### F2. 恢复（高风险，需确认）

```bash
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && \
  docker compose stop api && \
  docker compose exec -T postgres psql -U {DB_USER} -d {DB_NAME} < {backup_file} && \
  docker compose start api"
```

#### F3. 迁移状态

```bash
ssh {SSH_ALIAS} "cd {SERVICE_PATH}/deploy && \
  docker compose exec -T postgres psql -U {DB_USER} -d {DB_NAME} \
  -c \"SELECT version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank;\""
```

---

## 安全规则

| 规则 | 说明 |
|------|------|
| 不暴露密码 | SSH 命令中不包含明文密码，依赖 SSH key 认证 |
| 不使用 force | 禁止 `git push --force`、`git reset --hard`（除非用户明确要求回滚） |
| 备份先行 | 数据库恢复前必须先备份当前数据 |
| 确认高风险 | 回滚、数据库恢复必须展示计划并等待用户确认 |
| 超时处理 | SSH 命令加 timeout，避免无限等待 |

## 快速命令

- `部署` / `deploy` — 完整部署流程（模式 A）
- `服务状态` / `status` — 查看当前状态（模式 B）
- `查看日志` / `logs` — 查看容器日志（模式 C）
- `重启` / `restart` — 重启服务（模式 D）
- `回滚` / `rollback` — 回滚到指定版本（模式 E）
- `数据库备份` / `db backup` — 数据库备份（模式 F）
- `迁移状态` / `migration status` — 查看 Flyway 迁移历史（模式 F）
