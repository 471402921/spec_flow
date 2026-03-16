# 基础设施拓扑

## 架构总览

```
┌─────────────────────────────────────────────────────────┐
│  开发 Mac                                                │
│  ┌──────────────┐   git push    ┌──────────────┐        │
│  │ Local Dev    │ ────────────> │ GitHub       │        │
│  └──────┬───────┘               └──────────────┘        │
│         │ ssh specflow                                   │
└─────────┼───────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────┐
│  腾讯云轻量应用服务器（Ubuntu 24.04, 4C8G）               │
│  IP: 81.71.88.130                                        │
│                                                          │
│  ┌── Nginx (反向代理 + Let's Encrypt HTTPS) ──────────┐ │
│  │  443 → proxy_pass http://localhost:8080              │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                          │
│  ┌── Docker Compose ──────────────────────────────────┐  │
│  │  ┌─────────────┐  ┌───────────┐  ┌──────────────┐ │  │
│  │  │ postgres:16 │  │ redis:7   │  │ specflow-api │  │  │
│  │  │ :5432       │  │ :6379     │  │ :8080        │  │  │
│  │  │ vol:pgdata  │  │ vol:redis │  │ Java 21 JRE  │  │  │
│  │  └─────────────┘  └───────────┘  └──────────────┘  │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

## 连接方式

### SSH 到服务器

```bash
ssh specflow
# 等同于: ssh ubuntu@81.71.88.130 -i ~/.ssh/jet.pem
```

- 认证：SSH key（`~/.ssh/jet.pem`）
- 用户：`ubuntu`

## 容器配置

### docker-compose.yml 容器清单

| 容器 | 镜像 | 端口 | 数据卷 | 健康检查 |
|------|------|------|--------|---------|
| specflow-postgres | postgres:16 | 5432 | pgdata | `pg_isready -U specflow` |
| specflow-redis | redis:7-alpine | 6379 | redisdata | `redis-cli ping` |
| specflow-api | 本地构建 | 8080 | 无 | 依赖 postgres healthy |

### 环境变量

生产环境通过 `deploy/.env` 注入（基于 `.env.example`）：

| 变量 | 用途 | 默认值 |
|------|------|--------|
| `POSTGRES_DB` | 数据库名 | `specflow` |
| `POSTGRES_USER` | 数据库用户 | `specflow` |
| `POSTGRES_PASSWORD` | 数据库密码 | **无默认值** |
| `SPRING_PROFILES_ACTIVE` | Spring Profile | `dev` |

### Docker 镜像构建

多阶段构建（`deploy/Dockerfile`）：

1. **Builder**: `eclipse-temurin:21-jdk` + Maven 编译
2. **Runtime**: `eclipse-temurin:21-jre` + JAR 运行

### Docker Registry Mirrors

腾讯云镜像加速（`/etc/docker/daemon.json`）：

```json
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://ccr.ccs.tencentyun.com"
  ]
}
```

## 关键路径

### 服务器文件路径

| 路径 | 说明 |
|------|------|
| `/srv/specflow-service/` | 项目根目录（git clone） |
| `/srv/specflow-service/deploy/` | Docker Compose + Dockerfile + deploy.sh |
| `/srv/specflow-service/deploy/.env` | 环境变量（不入 git） |
| `/etc/nginx/sites-available/specflow` | Nginx 配置 |
| `/etc/docker/daemon.json` | Docker 镜像加速配置 |

### 公开访问 URL

| URL | 说明 |
|-----|------|
| `https://{domain}/actuator/health` | 健康检查 |
| `https://{domain}/swagger-ui/index.html` | Swagger UI |
| `https://{domain}/api/v1/` | API 根路径 |

> 域名配置完成后更新此表。

## deploy.sh 流程

```
git pull origin main
    ↓
docker compose down
    ↓
docker compose up -d --build
    ↓
sleep 15（等待容器启动）
    ↓
curl localhost:8080/actuator/health
    ↓
输出状态和访问 URL
```
