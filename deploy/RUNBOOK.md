# SpecFlow Service - 运维手册 (Runbook)

> **服务器**: 腾讯云轻量应用服务器 4C8G，Ubuntu 24.04，IP `81.71.88.130`
> **SSH**: `ssh specflow`（本地 `~/.ssh/config` 已配置，用户 `ubuntu`，密钥 `jet.pem`）
> **部署方式**: Docker Compose + Nginx 反向代理
> **项目路径**: `/srv/specflow-service`

---

## 📋 目录
- [快速参考](#快速参考)
- [首次部署](#首次部署)
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
# 进入项目部署目录
cd /srv/specflow-service/deploy

# 启动服务
docker compose up -d

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
```

---

## 🏗️ 首次部署

### 1. 服务器初始化

```bash
# 安装 Docker（国内服务器使用阿里云镜像源）
sudo apt-get update -qq && sudo apt-get install -y -qq ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://mirrors.aliyun.com/docker-ce/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update -qq && sudo apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER

# 配置 Docker Hub 镜像加速（国内服务器必需）
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json > /dev/null <<EOF
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://ccr.ccs.tencentyun.com"
  ]
}
EOF
sudo systemctl daemon-reload && sudo systemctl restart docker

# 克隆项目
sudo mkdir -p /srv/specflow-service
sudo chown $USER:$USER /srv/specflow-service
git clone https://github.com/471402921/spec_flow.git /srv/specflow-service
```

### 1.5 腾讯云防火墙配置

在腾讯云控制台 → 实例 → 防火墙，添加以下规则：

| 协议 | 端口 | 来源 | 备注 |
|------|------|------|------|
| TCP | 8080 | 全部IPv4 | API 服务 |
| TCP | 80 | 全部IPv4 | HTTP（已默认开放） |
| TCP | 443 | 全部IPv4 | HTTPS（已默认开放） |

### 2. 配置环境变量

```bash
cd /srv/specflow-service/deploy
cp .env.example .env
# 编辑 .env，修改数据库密码等敏感配置
vi .env
```

### 3. 启动服务

```bash
docker compose up -d
sleep 10
curl -f http://localhost:8080/actuator/health
```

### 4. 配置 Nginx + HTTPS（可选）

```bash
# 安装 Nginx 和 Certbot
sudo apt install -y nginx certbot python3-certbot-nginx

# 创建 Nginx 配置
sudo tee /etc/nginx/sites-available/specflow <<'EOF'
server {
    server_name specflow.example.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF

sudo ln -s /etc/nginx/sites-available/specflow /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx

# 申请 HTTPS 证书
sudo certbot --nginx -d specflow.example.com
```

---

## 🔧 服务启动与停止

### 1. 启动所有服务

```bash
cd /srv/specflow-service/deploy
docker compose up -d
```

**验证启动成功**:
```bash
docker compose ps
sleep 10
curl -f http://localhost:8080/actuator/health || echo "API not ready"
```

### 2. 停止所有服务

```bash
cd /srv/specflow-service/deploy
docker compose down
```

### 3. 重启单个服务

```bash
cd /srv/specflow-service/deploy

# 重启 API
docker compose restart api

# 重启数据库（会导致 API 短暂不可用）
docker compose restart postgres

# 重启 Redis
docker compose restart redis
```

### 4. 完全重建服务

```bash
cd /srv/specflow-service/deploy
docker compose down
docker compose up -d --build
sleep 10
curl -f http://localhost:8080/actuator/health
```

---

## 📦 发布升级流程

### 方式 1: 一键自动部署（推荐）

```bash
# 从本地推送代码后，SSH 到服务器执行
ssh specflow "cd /srv/specflow-service/deploy && bash deploy.sh"
```

`deploy.sh` 会自动执行：
1. 拉取最新代码 (`git pull origin main`)
2. 停止现有服务
3. 重建 Docker 镜像并启动
4. 健康检查

### 方式 2: 手动分步部署

```bash
cd /srv/specflow-service

# 1. 备份当前版本（可选）
git rev-parse HEAD > /tmp/specflow_last_commit.txt

# 2. 拉取最新代码
git pull origin main

# 3. 重建并启动服务
cd deploy
docker compose up -d --build

# 4. 验证
sleep 10
curl -f http://localhost:8080/actuator/health || echo "Health check failed!"

# 5. 查看日志确认无错误
docker compose logs --tail=50 api
```

### 发布后验证清单

- [ ] 容器状态正常: `docker compose ps`
- [ ] 健康检查通过: `curl http://localhost:8080/actuator/health`
- [ ] Swagger UI 可访问: `curl http://localhost:8080/swagger-ui/index.html`
- [ ] 数据库连接正常（health 响应中包含 `db: UP`）
- [ ] 日志无 ERROR 级别报错: `docker compose logs --tail=100 api | grep ERROR`

---

## ⏮️ 回滚流程

### 1. 代码回滚（快速）

```bash
cd /srv/specflow-service

# 查看最近的提交历史
git log --oneline -5

# 回滚到指定 commit
git reset --hard <commit-hash>

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
docker images | grep specflow-api
```

### 3. 数据库 Migration 回滚

**警告**: Flyway 不支持自动回滚，需要手动编写回滚脚本。

```bash
cd /srv/specflow-service/deploy

# 查看当前 migration 版本
docker compose exec postgres psql -U specflow -d specflow -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# 手动回滚（需要自己编写 SQL）
docker compose exec postgres psql -U specflow -d specflow
```

**最佳实践**:
- 重要 migration 前先备份数据库
- 生产环境慎用数据库回滚，优先考虑代码修复

---

## 💾 备份与恢复

### 数据库备份

#### 手动备份

```bash
cd /srv/specflow-service/deploy

BACKUP_FILE="backup_$(date +%Y%m%d_%H%M%S).sql"
docker compose exec -T postgres pg_dump -U specflow specflow > "$BACKUP_FILE"
echo "Backup created: $BACKUP_FILE"
```

#### 定期备份（Cron 任务）

```bash
crontab -e

# 每天凌晨 2 点备份，保留 7 天
0 2 * * * cd /srv/specflow-service/deploy && docker compose exec -T postgres pg_dump -U specflow specflow > backup_$(date +\%Y\%m\%d).sql && find . -name "backup_*.sql" -mtime +7 -delete
```

### 数据库恢复

```bash
cd /srv/specflow-service/deploy

# 1. 停止 API 服务
docker compose stop api

# 2. 恢复数据库
docker compose exec -T postgres psql -U specflow -d specflow < <backup-file.sql>

# 3. 重启 API 服务
docker compose start api

# 4. 验证
sleep 5
curl -f http://localhost:8080/actuator/health
```

---

## 🏥 健康检查

```bash
cd /srv/specflow-service/deploy

# 容器状态（预期: 3 个容器 running/healthy）
docker compose ps

# API 健康端点
curl http://localhost:8080/actuator/health

# 数据库连接
docker compose exec postgres psql -U specflow -d specflow -c "SELECT 1;"

# Redis 连接
docker compose exec redis redis-cli ping
```

---

## 📝 日志查看

```bash
cd /srv/specflow-service/deploy

# 实时查看 API 日志
docker compose logs -f api

# 查看最近 100 行
docker compose logs --tail=100 api

# 查看特定时间段
docker compose logs --since="2026-03-13T10:00:00" api

# 查找错误
docker compose logs api | grep -i error

# 查找特定 TraceId
docker compose logs api | grep "<traceId>"

# 系统资源使用
docker stats --no-stream
```

---

## 🔍 故障排查

### 问题 1: API 无法启动

**症状**: `docker compose ps` 显示 api 容器状态为 `Exit 1`

```bash
# 查看启动日志
docker compose logs api

# 常见原因: 数据库连接失败、端口被占用、环境变量缺失

# 检查数据库连接
docker compose exec postgres psql -U specflow -d specflow -c "SELECT 1;"

# 重启数据库
docker compose restart postgres
sleep 5
docker compose restart api
```

### 问题 2: 数据库连接超时

```bash
docker compose ps postgres
docker compose exec api env | grep DB_
docker compose exec postgres psql -U specflow -d specflow -c "\conninfo"
docker compose restart postgres
```

### 问题 3: 公网无法访问

```bash
# 1. 检查本地 API 是否正常
curl http://localhost:8080/actuator/health

# 2. 检查 Nginx 状态
sudo systemctl status nginx
sudo nginx -t

# 3. 检查 Nginx 错误日志
sudo tail -20 /var/log/nginx/error.log

# 4. 检查 HTTPS 证书
sudo certbot certificates
```

### 问题 4: Flyway Migration 失败

```bash
# 查看 migration 历史
docker compose exec postgres psql -U specflow -d specflow -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;"

# 修复方案 A: 清空 migration 历史（开发环境）
docker compose exec postgres psql -U specflow -d specflow -c "DROP TABLE flyway_schema_history CASCADE;"
docker compose restart api

# 修复方案 B: 手动修复 checksum（生产环境）
```

### 问题 5: 容器内存/CPU 过高

```bash
# 查看资源使用
docker stats --no-stream

# 限制 JVM 内存（在 docker-compose.yml 中添加）
# environment:
#   JAVA_OPTS: -Xmx512m -Xms256m
```

---

## 📚 相关文档

- [Docker Compose 配置](./docker-compose.yml)
- [自动化部署脚本](./deploy.sh)
- [环境变量模板](./.env.example)

---

**文档版本**: v2.0
**最后更新**: 2026-03-13
**维护者**: SpecFlow Team
