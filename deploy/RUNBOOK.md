# SoulPal Service - 运维手册 (Runbook)

> **环境**: 家庭节点 (home-node) WSL2/Ubuntu 20.04
> **域名**: https://api.soulpal.me
> **项目路径**: `/srv/soulpal-service`
> **部署方式**: Docker Compose + Cloudflare Tunnel

---

## 📋 目录
- [快速参考](#快速参考)
- [服务启动与停止](#服务启动与停止)
- [发布升级流程](#发布升级流程)
- [回滚流程](#回滚流程)
- [备份与恢复](#备份与恢复)
- [健康检查](#健康检查)
- [日志查看](#日志查看)
- [故障排查](#故障排查)

---

## 🚀 快速参考

### 常用命令速查表

```bash
# 登录服务器
ssh home-node

# 进入项目目录
cd /srv/soulpal-service

# 启动服务
cd deploy && docker compose up -d

# 查看服务状态
docker compose ps

# 查看 API 日志
docker compose logs -f api

# 重启 API 服务
docker compose restart api

# 停止所有服务
docker compose down

# 健康检查
curl http://localhost:8080/actuator/health
curl https://api.soulpal.me/actuator/health
```

---

## 🔧 服务启动与停止

### 1. 启动所有服务

```bash
ssh home-node
cd /srv/soulpal-service/deploy
docker compose up -d
```

**验证启动成功**:
```bash
# 查看容器状态（应显示 3 个容器 running/healthy）
docker compose ps

# 等待 10 秒后检查健康状态
sleep 10
curl -f http://localhost:8080/actuator/health || echo "API not ready"
```

### 2. 停止所有服务

```bash
cd /srv/soulpal-service/deploy
docker compose down
```

### 3. 重启单个服务

```bash
cd /srv/soulpal-service/deploy

# 重启 API（零停机时间较短）
docker compose restart api

# 重启数据库（会导致 API 短暂不可用）
docker compose restart postgres

# 重启 Redis
docker compose restart redis
```

### 4. 完全重建服务

```bash
cd /srv/soulpal-service/deploy

# 停止并删除容器
docker compose down

# 重新构建镜像并启动
docker compose up -d --build

# 等待并验证
sleep 10
curl -f http://localhost:8080/actuator/health
```

---

## 📦 发布升级流程

### 方式 1: 一键自动部署（推荐）

**从本地 Mac 执行**:
```bash
# 确保本地代码已提交并推送到 GitHub
git push origin main

# 远程部署
ssh home-node "cd /srv/soulpal-service/deploy && bash deploy.sh"
```

**从 home-node 执行**:
```bash
ssh home-node
cd /srv/soulpal-service/deploy
bash deploy.sh
```

`deploy.sh` 会自动执行：
1. 拉取最新代码 (`git pull origin main`)
2. 使用 Maven 构建 (`./mvnw clean package -DskipTests`)
3. 重建 Docker 镜像
4. 重启服务 (`docker compose up -d --build`)
5. 健康检查

### 方式 2: 手动分步部署

```bash
ssh home-node
cd /srv/soulpal-service

# 1. 备份当前版本（可选）
git rev-parse HEAD > /tmp/soulpal_last_commit.txt

# 2. 拉取最新代码
git pull origin main

# 3. 构建项目
./mvnw clean package -DskipTests

# 4. 重建并启动服务
cd deploy
docker compose up -d --build

# 5. 等待服务启动
sleep 10

# 6. 健康检查
curl -f http://localhost:8080/actuator/health || echo "❌ Health check failed!"

# 7. 公网验证
curl -f https://api.soulpal.me/actuator/health || echo "❌ Public health check failed!"

# 8. 查看日志确认无错误
docker compose logs --tail=50 api
```

### 发布后验证清单

- [ ] 容器状态正常: `docker compose ps`
- [ ] 本地健康检查通过: `curl http://localhost:8080/actuator/health`
- [ ] 公网健康检查通过: `curl https://api.soulpal.me/actuator/health`
- [ ] Swagger UI 可访问: `curl https://api.soulpal.me/swagger-ui/index.html`
- [ ] 数据库连接正常（health 响应中包含 `db: UP`）
- [ ] 日志无 ERROR 级别报错: `docker compose logs --tail=100 api | grep ERROR`

---

## ⏮️ 回滚流程

### 1. 代码回滚（快速）

```bash
ssh home-node
cd /srv/soulpal-service

# 查看最近的提交历史
git log --oneline -5

# 回滚到指定 commit（替换 <commit-hash>）
git reset --hard <commit-hash>

# 或回滚到上一个版本
git reset --hard HEAD~1

# 重新构建并部署
cd deploy
docker compose up -d --build

# 验证
sleep 10
curl -f http://localhost:8080/actuator/health
```

### 2. Docker 镜像回滚

```bash
# 查看镜像历史
docker images | grep soulpal-api

# 如果之前的镜像还在，可以手动修改 docker-compose.yml
# 临时指定旧镜像 ID，然后 docker compose up -d
```

### 3. 数据库 Migration 回滚

**警告**: Flyway 不支持自动回滚，需要手动编写回滚脚本。

```bash
# 查看当前 migration 版本
ssh home-node
cd /srv/soulpal-service/deploy
docker compose exec postgres psql -U soulpal -d soulpal -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# 手动回滚（需要自己编写 SQL）
docker compose exec postgres psql -U soulpal -d soulpal

# 在 psql 中执行回滚 SQL（示例）
-- 例如删除 V1.0 创建的表
-- DROP TABLE IF EXISTS session CASCADE;
-- DELETE FROM flyway_schema_history WHERE version = '1.0';
```

**最佳实践**:
- 编写可逆的 migration（带 undo 脚本）
- 重要 migration 前先备份数据库
- 生产环境慎用数据库回滚，优先考虑代码修复

---

## 💾 备份与恢复

### 数据库备份

#### 手动备份（立即执行）

```bash
ssh home-node
cd /srv/soulpal-service/deploy

# 备份到本地文件（带时间戳）
BACKUP_FILE="backup_$(date +%Y%m%d_%H%M%S).sql"
docker compose exec -T postgres pg_dump -U soulpal soulpal > "$BACKUP_FILE"

echo "Backup created: $BACKUP_FILE"
ls -lh $BACKUP_FILE
```

#### 定期备份（Cron 任务）

```bash
# 在 home-node 上设置每天凌晨 2 点备份
ssh home-node
crontab -e

# 添加以下行
0 2 * * * cd /srv/soulpal-service/deploy && docker compose exec -T postgres pg_dump -U soulpal soulpal > backup_$(date +\%Y\%m\%d).sql && find . -name "backup_*.sql" -mtime +7 -delete
```

### 数据库恢复

#### 从备份文件恢复

```bash
ssh home-node
cd /srv/soulpal-service/deploy

# 1. 停止 API 服务（避免写入冲突）
docker compose stop api

# 2. 恢复数据库（替换 <backup-file.sql>）
docker compose exec -T postgres psql -U soulpal -d soulpal < backup_20260207_020000.sql

# 3. 重启 API 服务
docker compose start api

# 4. 验证
sleep 5
curl -f http://localhost:8080/actuator/health
```

#### 完全重建数据库（危险操作）

```bash
ssh home-node
cd /srv/soulpal-service/deploy

# 1. 备份当前数据（保险措施）
docker compose exec -T postgres pg_dump -U soulpal soulpal > backup_before_reset.sql

# 2. 删除数据库
docker compose exec postgres psql -U soulpal -c "DROP DATABASE IF EXISTS soulpal;"
docker compose exec postgres psql -U soulpal -c "CREATE DATABASE soulpal;"

# 3. 重启服务（Flyway 会重新初始化）
docker compose restart api

# 4. 验证
sleep 10
curl -f http://localhost:8080/actuator/health
```

### 备份文件管理

```bash
# 查看所有备份文件
ssh home-node "ls -lh /srv/soulpal-service/deploy/backup_*.sql"

# 删除 7 天前的旧备份
ssh home-node "find /srv/soulpal-service/deploy -name 'backup_*.sql' -mtime +7 -delete"

# 下载备份到本地 Mac
scp home-node:/srv/soulpal-service/deploy/backup_20260207_020000.sql ~/Downloads/
```

---

## 🏥 健康检查

### 本地健康检查

```bash
ssh home-node

# 1. 容器状态
cd /srv/soulpal-service/deploy
docker compose ps
# 预期: 3 个容器 (api, postgres, redis) 状态为 running/healthy

# 2. API 健康端点
curl http://localhost:8080/actuator/health
# 预期: {"status":"UP", "components": {"db":{"status":"UP"}, ...}}

# 3. 数据库连接
docker compose exec postgres psql -U soulpal -d soulpal -c "SELECT 1;"
# 预期: 返回 1

# 4. Redis 连接
docker compose exec redis redis-cli ping
# 预期: PONG
```

### 公网健康检查

```bash
# 1. Cloudflare Tunnel 连接状态
curl https://api.soulpal.me/cdn-cgi/trace
# 预期: 包含 colo=SJC 等 Cloudflare 信息

# 2. API 健康端点
curl https://api.soulpal.me/actuator/health
# 预期: {"status":"UP"}

# 3. Swagger UI 可访问性
curl -I https://api.soulpal.me/swagger-ui/index.html
# 预期: HTTP/2 200

# 4. 测试 API 调用
curl https://api.soulpal.me/api/v1/hello
# 预期: JSON 响应
```

### Cloudflare Tunnel 检查

```bash
ssh home-node

# 查看 cloudflared 服务状态
sudo systemctl status cloudflared

# 重启 cloudflared（如果连接异常）
sudo systemctl restart cloudflared

# 查看 Tunnel 配置
cat /etc/cloudflared/config.yml
```

---

## 📝 日志查看

### 应用日志

```bash
ssh home-node
cd /srv/soulpal-service/deploy

# 实时查看 API 日志（推荐）
docker compose logs -f api

# 查看最近 100 行
docker compose logs --tail=100 api

# 查看所有服务日志
docker compose logs -f

# 查看特定时间段的日志
docker compose logs --since="2026-02-07T10:00:00" api

# 导出日志到文件
docker compose logs --no-color api > /tmp/api_logs.txt
```

### 数据库日志

```bash
ssh home-node
cd /srv/soulpal-service/deploy

# 查看 PostgreSQL 日志
docker compose logs --tail=50 postgres

# 查看慢查询（如果已配置）
docker compose exec postgres psql -U soulpal -d soulpal -c "SELECT * FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"
```

### 系统日志

```bash
ssh home-node

# Docker daemon 日志
sudo journalctl -u docker --since "1 hour ago"

# Cloudflared 日志
sudo journalctl -u cloudflared --since "1 hour ago" -f

# 系统资源使用
docker stats --no-stream
```

### 日志分析常用命令

```bash
# 查找错误
docker compose logs api | grep -i error

# 统计错误数量
docker compose logs --since="1 hour ago" api | grep -i error | wc -l

# 查找特定 TraceId 的日志
docker compose logs api | grep "abc-123-def"

# 查找慢请求（假设日志包含耗时）
docker compose logs api | grep "took more than"
```

---

## 🔍 故障排查

### 问题 1: API 无法启动

**症状**: `docker compose ps` 显示 api 容器状态为 `Exit 1`

**排查步骤**:
```bash
# 1. 查看启动日志
docker compose logs api

# 2. 常见原因
# - 数据库连接失败 → 检查 postgres 容器状态
# - 端口被占用 → lsof -i :8080
# - 环境变量缺失 → 检查 .env 文件

# 3. 检查数据库连接
docker compose exec postgres psql -U soulpal -d soulpal -c "SELECT 1;"

# 4. 重启数据库
docker compose restart postgres
sleep 5
docker compose restart api
```

### 问题 2: 数据库连接超时

**症状**: API 日志显示 `Connection timeout` 或 `FATAL: password authentication failed`

**排查步骤**:
```bash
# 1. 检查 postgres 容器状态
docker compose ps postgres

# 2. 检查环境变量
docker compose exec api env | grep DB_

# 3. 测试数据库连接
docker compose exec postgres psql -U soulpal -d soulpal -c "\conninfo"

# 4. 重启数据库（如果死锁）
docker compose restart postgres
```

### 问题 3: 公网无法访问

**症状**: `curl https://api.soulpal.me/actuator/health` 返回 404 或超时

**排查步骤**:
```bash
# 1. 检查本地 API 是否正常
curl http://localhost:8080/actuator/health

# 2. 检查 Cloudflare Tunnel 状态
sudo systemctl status cloudflared

# 3. 重启 Tunnel
sudo systemctl restart cloudflared

# 4. 检查 Tunnel 配置
cat /etc/cloudflared/config.yml
# 确认 url: http://localhost:8080

# 5. 测试 Cloudflare 连接
curl https://api.soulpal.me/cdn-cgi/trace
```

### 问题 4: Flyway Migration 失败

**症状**: API 启动时报 `FlywayException: Validate failed`

**排查步骤**:
```bash
# 1. 查看 migration 历史
docker compose exec postgres psql -U soulpal -d soulpal -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;"

# 2. 检查 SQL 文件
ls -la soulpal-api/src/main/resources/db/migration/

# 3. 修复方案 A: 清空 migration 历史（开发环境）
docker compose exec postgres psql -U soulpal -d soulpal -c "DROP TABLE flyway_schema_history CASCADE;"
docker compose restart api

# 4. 修复方案 B: 手动修复 checksum（生产环境）
# 手动更新 flyway_schema_history 表的 checksum 字段
```

### 问题 5: Docker 镜像拉取失败（中国网络）

**症状**: `docker compose up` 报 `Error response from daemon: Get https://registry-1.docker.io/v2/: net/http: TLS handshake timeout`

**解决方案**:
```bash
# 1. 检查镜像加速器配置
cat /etc/docker/daemon.json
# 应包含: "registry-mirrors": ["https://docker.1ms.run", "https://docker.xuanyuan.me"]

# 2. 重启 Docker daemon
sudo systemctl restart docker

# 3. 测试拉取
docker pull postgres:16
```

### 问题 6: 容器内存/CPU 过高

```bash
# 查看资源使用
docker stats --no-stream

# 查看 Java heap 使用（API 容器内）
docker compose exec api jstat -gc 1

# 临时限制容器资源（修改 docker-compose.yml）
# services:
#   api:
#     deploy:
#       resources:
#         limits:
#           cpus: '1.0'
#           memory: 512M
```

---

## 📞 紧急联系方式

- **项目负责人**: [填写联系方式]
- **运维负责人**: [填写联系方式]
- **GitHub Issues**: https://github.com/471402921/soulpal_service/issues

---

## 📚 相关文档

- [项目启动记录](../soulpal_plan/项目启动记录.md)
- [部署架构文档](../architecture/部署架构.md)
- [Docker Compose 配置](./docker-compose.yml)
- [自动化部署脚本](./deploy.sh)
- [API 文档](https://api.soulpal.me/swagger-ui/index.html)

---

## 📋 部署历史

| 日期 | Commit | 内容 | 状态 |
|------|--------|------|------|
| 2026-02-14 | `633e45e` | 完成 User 模块（P1+P2）+ Family 模块 | ✅ 成功 |

---

**文档版本**: v1.1
**最后更新**: 2026-02-14
**维护者**: SoulPal Team
