# 故障排查手册

## 快速诊断流程

遇到问题时按此顺序排查：

```
1. 容器是否运行？    → docker compose ps
2. API 是否健康？    → curl localhost:8080/actuator/health
3. 日志有无报错？    → docker compose logs --tail=50 api
4. 数据库可连接？    → docker compose exec -T postgres pg_isready -U specflow
5. Tunnel 是否通？   → curl https://api.specflow.dev/actuator/health
6. Tunnel 服务状态？ → sudo systemctl status cloudflared
```

## 常见故障

### 1. API 容器无法启动

**症状**: `docker compose ps` 显示 api 容器 Exit 或 Restarting

**排查**:

```bash
# 查看启动日志
ssh home-node "cd /srv/specflow-service/deploy && docker compose logs --tail=100 api"

# 检查 Postgres 健康状态（API 依赖 Postgres healthy）
ssh home-node "cd /srv/specflow-service/deploy && docker compose ps postgres"
```

**常见原因与解决**:

| 原因 | 日志特征 | 解决 |
|------|---------|------|
| Postgres 未就绪 | `Connection refused` | 等待 Postgres healthy 后手动启动: `docker compose up -d api` |
| 环境变量缺失 | `DB_PASSWORD` not set | 检查 `deploy/.env` 文件是否存在且内容正确 |
| Flyway 迁移失败 | `FlywayException` | 查看具体迁移错误，修复 SQL 后重新部署 |
| JAR 构建失败 | `mvn` 相关错误 | 本地先跑 `./mvnw clean package -DskipTests` 确认编译通过 |
| 内存不足 | `OutOfMemoryError` | 检查宿主机内存: `free -h` |

### 2. 数据库连接失败

**症状**: API 日志出现 `Connection refused` 或 `timeout`

**排查**:

```bash
# Postgres 容器是否运行
ssh home-node "cd /srv/specflow-service/deploy && docker compose ps postgres"

# 是否能连上
ssh home-node "cd /srv/specflow-service/deploy && \
  docker compose exec -T postgres pg_isready -U specflow"

# 查看 Postgres 日志
ssh home-node "cd /srv/specflow-service/deploy && docker compose logs --tail=50 postgres"

# 检查环境变量
ssh home-node "cd /srv/specflow-service/deploy && docker compose exec -T api env | grep DB_"
```

**常见原因与解决**:

| 原因 | 解决 |
|------|------|
| Postgres 容器挂了 | `docker compose up -d postgres`，等 healthy 后重启 api |
| 密码不匹配 | 对比 `.env` 和 `application-prod.yml` 的密码配置 |
| 数据卷损坏 | `docker compose down -v`（**会清除数据**，确认前先备份）|

### 3. 外网无法访问（Cloudflare Tunnel）

**症状**: 内网 `curl localhost:8080` 正常，但 `curl https://api.specflow.dev` 失败

**排查**:

```bash
# Tunnel 服务状态
ssh home-node "sudo systemctl status cloudflared --no-pager -l"

# Tunnel 日志
ssh home-node "sudo journalctl -u cloudflared --since '30 min ago' --no-pager"

# Tunnel 配置是否正确
ssh home-node "cat /etc/cloudflared/config.yml"

# 凭证文件是否存在
ssh home-node "ls -la /etc/cloudflared/*.json"
```

**常见原因与解决**:

| 原因 | 解决 |
|------|------|
| cloudflared 服务未启动 | `sudo systemctl start cloudflared` |
| cloudflared 未开机自启 | `sudo systemctl enable cloudflared` |
| 凭证文件权限错误 | `sudo chmod 644 /etc/cloudflared/*.json` |
| config.yml 语法错误 | 检查 YAML 缩进，修复后 `sudo systemctl restart cloudflared` |
| Cloudflare DNS 配置 | 登录 Cloudflare Dashboard 检查 DNS CNAME 记录 |

### 4. Docker 拉取镜像超时

**症状**: `docker compose up --build` 卡在拉取基础镜像

**排查**:

```bash
# 检查 Docker mirror 配置
ssh home-node "cat /etc/docker/daemon.json"

# 手动测试拉取
ssh home-node "docker pull eclipse-temurin:21-jre"
```

**解决**: 更新镜像加速地址或直接用代理。

### 5. Docker 服务未运行（WSL2 重启后）

**症状**: `Cannot connect to the Docker daemon`

```bash
# 启动 Docker
ssh home-node "echo 'pw' | sudo -S service docker start"

# 同时检查 Tailscale（也需要手动启动）
ssh home-node "echo 'pw' | sudo -S nohup tailscaled > /tmp/tailscaled.log 2>&1 &"
ssh home-node "echo 'pw' | sudo -S tailscale up"
```

### 6. SSH 连接失败

**症状**: `ssh home-node` 超时或拒绝

**排查**:

```bash
# 检查 Tailscale 状态（本地 Mac）
tailscale status

# 直连 IP 测试
ssh ssccddjjjj@100.104.158.31

# 检查 WSL2 是否在运行（需要在 Windows 上操作）
```

**常见原因**:
- Windows 主机睡眠/关机 → 唤醒/开机
- WSL2 未启动 → Windows 端运行 `wsl`
- Tailscale 断连 → 两端都检查 `tailscale status`

## 日志查看命令集

### API 日志

```bash
# 最近 N 行
ssh home-node "cd /srv/specflow-service/deploy && docker compose logs --tail={N} api"

# 实时跟踪
ssh home-node "cd /srv/specflow-service/deploy && docker compose logs -f --tail=20 api"

# 只看错误
ssh home-node "cd /srv/specflow-service/deploy && docker compose logs api 2>&1 | grep -i 'error\|exception'"

# 按 TraceId 查询
ssh home-node "cd /srv/specflow-service/deploy && docker compose logs api 2>&1 | grep '{traceId}'"

# 时间范围（Docker 原生支持）
ssh home-node "cd /srv/specflow-service/deploy && docker compose logs --since '2h' api"
```

### 数据库日志

```bash
ssh home-node "cd /srv/specflow-service/deploy && docker compose logs --tail=50 postgres"
```

### 系统日志

```bash
# Cloudflare Tunnel
ssh home-node "sudo journalctl -u cloudflared --since '1 hour ago' --no-pager"

# Docker daemon
ssh home-node "sudo journalctl -u docker --since '1 hour ago' --no-pager"
```

## 回滚流程

### 代码回滚

```bash
# 1. 查看历史版本
ssh home-node "cd /srv/specflow-service && git log --oneline -10"

# 2. 回滚到指定 commit
ssh home-node "cd /srv/specflow-service && git checkout {commit-hash}"

# 3. 重新构建并部署
ssh home-node "cd /srv/specflow-service/deploy && docker compose up -d --build"

# 4. 验证
ssh home-node "cd /srv/specflow-service/deploy && docker compose ps"
ssh home-node "curl -s http://localhost:8080/actuator/health"
```

### 数据库回滚

Flyway 不支持自动 undo，需手动处理：

```bash
# 1. 查看当前迁移状态
ssh home-node "cd /srv/specflow-service/deploy && \
  docker compose exec -T postgres psql -U specflow -d specflow \
  -c \"SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;\""

# 2. 先备份
ssh home-node "cd /srv/specflow-service/deploy && \
  docker compose exec -T postgres pg_dump -U specflow specflow > backup_before_rollback.sql"

# 3. 手动执行 undo SQL（需要人工编写）
ssh home-node "cd /srv/specflow-service/deploy && \
  docker compose exec -T postgres psql -U specflow -d specflow -c \"{undo_sql}\""

# 4. 删除 Flyway 记录
ssh home-node "cd /srv/specflow-service/deploy && \
  docker compose exec -T postgres psql -U specflow -d specflow \
  -c \"DELETE FROM flyway_schema_history WHERE version = '{version}';\""

# 5. 重启 API 使 Flyway 状态同步
ssh home-node "cd /srv/specflow-service/deploy && docker compose restart api"
```

## WSL2 重启后恢复清单

WSL2 每次重启后需要手动恢复以下服务：

```bash
# 1. 启动 Docker
echo 'pw' | sudo -S service docker start

# 2. 启动 Tailscale
sudo nohup tailscaled > /tmp/tailscaled.log 2>&1 &
sudo tailscale up

# 3. 启动应用容器
cd /srv/specflow-service/deploy && docker compose up -d

# 4. 验证
docker compose ps
curl -s http://localhost:8080/actuator/health

# 5. Cloudflare Tunnel（通常已设为 systemd 自启）
sudo systemctl status cloudflared
```
