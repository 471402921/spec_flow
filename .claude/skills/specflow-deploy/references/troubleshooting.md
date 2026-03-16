# 故障排查手册

## 快速诊断流程

遇到问题时按此顺序排查：

```
1. 容器是否运行？    → docker compose ps
2. API 是否健康？    → curl localhost:8080/actuator/health
3. 日志有无报错？    → docker compose logs --tail=50 api
4. 数据库可连接？    → docker compose exec -T postgres pg_isready -U specflow
5. Nginx 是否正常？  → sudo systemctl status nginx && sudo nginx -t
6. 公网可达？        → curl https://{domain}/actuator/health
```

## 常见故障

### 1. API 容器无法启动

**症状**: `docker compose ps` 显示 api 容器 Exit 或 Restarting

**排查**:

```bash
# 查看启动日志
ssh specflow "cd /srv/specflow-service/deploy && docker compose logs --tail=100 api"

# 检查 Postgres 健康状态（API 依赖 Postgres healthy）
ssh specflow "cd /srv/specflow-service/deploy && docker compose ps postgres"
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
ssh specflow "cd /srv/specflow-service/deploy && docker compose ps postgres"
ssh specflow "cd /srv/specflow-service/deploy && docker compose exec -T postgres pg_isready -U specflow"
ssh specflow "cd /srv/specflow-service/deploy && docker compose logs --tail=50 postgres"
```

**常见原因与解决**:

| 原因 | 解决 |
|------|------|
| Postgres 容器挂了 | `docker compose up -d postgres`，等 healthy 后重启 api |
| 密码不匹配 | 对比 `.env` 和 `application-prod.yml` 的密码配置 |
| 数据卷损坏 | `docker compose down -v`（**会清除数据**，确认前先备份）|

### 3. 公网无法访问（Nginx）

**症状**: 内网 `curl localhost:8080` 正常，但公网访问失败

**排查**:

```bash
# Nginx 状态
ssh specflow "sudo systemctl status nginx --no-pager -l"

# Nginx 配置检查
ssh specflow "sudo nginx -t"

# Nginx 错误日志
ssh specflow "sudo tail -20 /var/log/nginx/error.log"

# HTTPS 证书状态
ssh specflow "sudo certbot certificates"
```

**常见原因与解决**:

| 原因 | 解决 |
|------|------|
| Nginx 未启动 | `sudo systemctl start nginx` |
| 配置语法错误 | `sudo nginx -t` 检查，修复后 `sudo systemctl reload nginx` |
| 证书过期 | `sudo certbot renew` |
| 防火墙未开放 | 腾讯云控制台检查 80/443 端口规则 |

### 4. Docker 拉取镜像超时

**症状**: `docker compose up --build` 卡在拉取基础镜像

**排查**:

```bash
ssh specflow "cat /etc/docker/daemon.json"
ssh specflow "docker pull eclipse-temurin:21-jre"
```

**解决**: 更新镜像加速地址或直接用代理。

### 5. GitHub 拉取代码超时

**症状**: `git pull` 卡住或报错 `Failed to connect to github.com`

**解决**: 国内服务器访问 GitHub 不稳定，重试即可。

## 日志查看命令集

### API 日志

```bash
# 最近 N 行
ssh specflow "cd /srv/specflow-service/deploy && docker compose logs --tail={N} api"

# 实时跟踪
ssh specflow "cd /srv/specflow-service/deploy && docker compose logs -f --tail=20 api"

# 只看错误
ssh specflow "cd /srv/specflow-service/deploy && docker compose logs api 2>&1 | grep -i 'error\|exception'"

# 按 TraceId 查询
ssh specflow "cd /srv/specflow-service/deploy && docker compose logs api 2>&1 | grep '{traceId}'"

# 时间范围
ssh specflow "cd /srv/specflow-service/deploy && docker compose logs --since '2h' api"
```

### 数据库日志

```bash
ssh specflow "cd /srv/specflow-service/deploy && docker compose logs --tail=50 postgres"
```

### Nginx 日志

```bash
ssh specflow "sudo tail -50 /var/log/nginx/access.log"
ssh specflow "sudo tail -50 /var/log/nginx/error.log"
```

## 回滚流程

### 代码回滚

```bash
# 1. 查看历史版本
ssh specflow "cd /srv/specflow-service && git log --oneline -10"

# 2. 回滚到指定 commit
ssh specflow "cd /srv/specflow-service && git checkout {commit-hash}"

# 3. 重新构建并部署
ssh specflow "cd /srv/specflow-service/deploy && docker compose up -d --build"

# 4. 验证
ssh specflow "cd /srv/specflow-service/deploy && docker compose ps"
ssh specflow "curl -s http://localhost:8080/actuator/health"
```

### 数据库回滚

Flyway 不支持自动 undo，需手动处理：

```bash
# 1. 查看当前迁移状态
ssh specflow "cd /srv/specflow-service/deploy && \
  docker compose exec -T postgres psql -U specflow -d specflow \
  -c \"SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;\""

# 2. 先备份
ssh specflow "cd /srv/specflow-service/deploy && \
  docker compose exec -T postgres pg_dump -U specflow specflow > backup_before_rollback.sql"

# 3. 手动执行 undo SQL（需要人工编写）
ssh specflow "cd /srv/specflow-service/deploy && \
  docker compose exec -T postgres psql -U specflow -d specflow -c \"{undo_sql}\""

# 4. 删除 Flyway 记录
ssh specflow "cd /srv/specflow-service/deploy && \
  docker compose exec -T postgres psql -U specflow -d specflow \
  -c \"DELETE FROM flyway_schema_history WHERE version = '{version}';\""

# 5. 重启 API
ssh specflow "cd /srv/specflow-service/deploy && docker compose restart api"
```
