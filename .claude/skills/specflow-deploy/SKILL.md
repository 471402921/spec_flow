---
name: specflow-deploy
description: SpecFlow Service 部署操作技能 - 封装从本地构建到远程部署的完整流程，包括部署前检查、执行部署、健康验证、日志查看、回滚和数据库操作。当用户请求部署相关操作(部署, deploy, 发布, 上线, 回滚, rollback, 查看日志, logs, 重启, restart, 数据库备份, db backup, 健康检查, health check, 服务状态, status)时触发此技能。
---

# SpecFlow Service 部署操作

你是 SpecFlow Service 项目的部署运维助手。负责执行部署、验证健康状态、排查问题和执行回滚。

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

# 2. 运行 checkstyle
./mvnw checkstyle:check

# 3. 运行测试
./mvnw clean test

# 4. 确认没有未提交的变更
```

**任何检查失败 → 终止部署**，报告失败原因。

#### A2. 推送代码

```bash
git push origin main
```

#### A3. 远程部署

```bash
ssh home-node "cd /srv/specflow-service/deploy && bash deploy.sh"
```

`deploy.sh` 会自动执行：git pull → docker compose down → docker compose up --build → 健康检查

#### A4. 部署验证

```bash
# 容器状态
ssh home-node "cd /srv/specflow-service/deploy && docker compose ps"

# 内网健康检查
ssh home-node "curl -s http://localhost:8080/actuator/health"

# 外网健康检查（Cloudflare Tunnel）
curl -s https://api.specflow.dev/actuator/health
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
| specflow-postgres | healthy | 5432 |
| specflow-redis | healthy | 6379 |
| specflow-api | running | 8080 |

### 健康检查
- 内网: ✅ `{"status":"UP"}`
- 外网: ✅ `{"status":"UP"}`
```

---

### 模式 B：服务状态查看

触发词：`服务状态`, `status`, `健康检查`, `health check`

> 快速查看当前部署状态，不做任何修改。

```bash
# 容器状态
ssh home-node "cd /srv/specflow-service/deploy && docker compose ps"

# 当前部署版本
ssh home-node "cd /srv/specflow-service && git log --oneline -1"

# 健康检查
ssh home-node "curl -s http://localhost:8080/actuator/health"

# Cloudflare Tunnel 状态
ssh home-node "sudo systemctl status cloudflared --no-pager -l"

# 外网可达性
curl -s https://api.specflow.dev/actuator/health
```

---

### 模式 C：日志查看

触发词：`查看日志`, `logs`, `日志`

> 查看容器日志，支持按容器、时间范围、关键词过滤。

```bash
# API 日志（最近 100 行）
ssh home-node "cd /srv/specflow-service/deploy && docker compose logs --tail=100 api"

# 实时跟踪 API 日志
ssh home-node "cd /srv/specflow-service/deploy && docker compose logs -f --tail=50 api"

# 查看错误日志
ssh home-node "cd /srv/specflow-service/deploy && docker compose logs api | grep -i error"

# 按 TraceId 查询
ssh home-node "cd /srv/specflow-service/deploy && docker compose logs api | grep '{traceId}'"

# Postgres 日志
ssh home-node "cd /srv/specflow-service/deploy && docker compose logs --tail=50 postgres"

# Cloudflare Tunnel 日志
ssh home-node "sudo journalctl -u cloudflared --since '1 hour ago' --no-pager"
```

根据用户需求选择合适的命令组合。

---

### 模式 D：重启服务

触发词：`重启`, `restart`

> 重启指定容器或全部服务。

```bash
# 仅重启 API（不重建镜像）
ssh home-node "cd /srv/specflow-service/deploy && docker compose restart api"

# 重建并重启 API
ssh home-node "cd /srv/specflow-service/deploy && docker compose up -d --build api"

# 重启全部服务
ssh home-node "cd /srv/specflow-service/deploy && docker compose restart"
```

重启后必须执行健康检查验证。

---

### 模式 E：回滚

触发词：`回滚`, `rollback`

> 将线上代码回退到指定版本。**高风险操作，必须用户确认。**

#### E1. 展示可回滚版本

```bash
ssh home-node "cd /srv/specflow-service && git log --oneline -10"
```

#### E2. 用户确认目标版本后执行

```bash
ssh home-node "cd /srv/specflow-service && \
  git checkout {commit-hash} && \
  cd deploy && docker compose up -d --build"
```

> **注意**：回滚不处理数据库迁移。如果回滚涉及 Flyway 迁移变更，需要在 E3 中手动处理。

#### E3. 数据库回滚（如需要）

仅在回滚跨越了 Flyway 迁移版本时需要：

```bash
# 检查当前迁移状态
ssh home-node "cd /srv/specflow-service/deploy && \
  docker compose exec -T postgres psql -U specflow -d specflow \
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
ssh home-node "cd /srv/specflow-service/deploy && \
  docker compose exec -T postgres pg_dump -U specflow specflow > backup_\$(date +%Y%m%d_%H%M%S).sql"
```

#### F2. 恢复（高风险，需确认）

```bash
ssh home-node "cd /srv/specflow-service/deploy && \
  docker compose stop api && \
  docker compose exec -T postgres psql -U specflow -d specflow < {backup_file} && \
  docker compose start api"
```

#### F3. 迁移状态

```bash
ssh home-node "cd /srv/specflow-service/deploy && \
  docker compose exec -T postgres psql -U specflow -d specflow \
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
