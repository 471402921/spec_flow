# 基础设施拓扑

## 架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│  开发 Mac                                                       │
│  ┌──────────────┐   git push    ┌──────────────┐               │
│  │ Local Dev    │ ────────────> │ GitHub       │               │
│  │ ./mvnw test  │               │ CI Pipeline  │               │
│  └──────┬───────┘               └──────────────┘               │
│         │ ssh home-node (Tailscale)                              │
└─────────┼───────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────┐
│  Home Node (WSL2/Ubuntu 20.04)                                   │
│  Tailscale IP: 100.104.158.31                                    │
│                                                                 │
│  ┌── Docker Compose ──────────────────────────────────┐         │
│  │                                                     │         │
│  │  ┌─────────────┐  ┌───────────┐  ┌──────────────┐ │         │
│  │  │ postgres:16 │  │ redis:7   │  │ specflow-api  │ │         │
│  │  │ :5432       │  │ :6379     │  │ :8080        │ │         │
│  │  │ vol:pgdata  │  │ vol:redis │  │ Java 21 JRE  │ │         │
│  │  └─────────────┘  └───────────┘  └──────┬───────┘ │         │
│  └─────────────────────────────────────────┼─────────┘         │
│                                             │                    │
│  ┌── Cloudflare Tunnel (cloudflared) ───────┘                   │
│  │  tunnel: home-dev                                            │
│  │  hostname: api.specflow.dev → localhost:8080                   │
│  └──────────────────────────────────────────────────────────────│
└─────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────┐
│  Cloudflare Edge                                                 │
│  TLS 终止 → https://api.specflow.dev                              │
│  → Swagger UI: /swagger-ui/index.html                           │
│  → Health:     /actuator/health                                  │
│  → API:        /api/v1/*                                        │
└─────────────────────────────────────────────────────────────────┘
```

## 连接方式

### SSH 到 Home Node

```bash
ssh home-node
# 等同于: ssh ssccddjjjj@100.104.158.31
```

- 认证：SSH key（无需密码）
- sudo 需要：`echo 'pw' | sudo -S {command}`（非交互模式无 TTY）

### Tailscale 网络

| 节点 | IP | 说明 |
|------|-----|------|
| Windows 主机 | `100.66.161.47` | Tailscale 客户端 |
| WSL2 (home-node) | `100.104.158.31` | 独立安装 tailscaled |

WSL2 的 tailscaled 需手动启动（重启后不会自动运行）：

```bash
sudo nohup tailscaled > /tmp/tailscaled.log 2>&1 &
sudo tailscale up
```

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

构建参数：`MODULE=specflow-api`（支持未来多模块）

### Docker Registry Mirrors

Home node 配置了国内镜像加速（`/etc/docker/daemon.json`）：

```json
{
  "registry-mirrors": [
    "https://docker.1ms.run",
    "https://docker.xuanyuan.me"
  ]
}
```

## 关键路径

### Home Node 文件路径

| 路径 | 说明 |
|------|------|
| `/srv/specflow-service/` | 项目根目录（git clone） |
| `/srv/specflow-service/deploy/` | Docker Compose + Dockerfile + deploy.sh |
| `/srv/specflow-service/deploy/.env` | 环境变量（不入 git） |
| `/etc/cloudflared/config.yml` | Cloudflare Tunnel 配置 |
| `/etc/cloudflared/{tunnel-id}.json` | Tunnel 凭证文件 |
| `/etc/docker/daemon.json` | Docker 镜像加速配置 |
| `/tmp/tailscaled.log` | Tailscale 守护进程日志 |

### Cloudflare Tunnel 配置

```yaml
tunnel: b123f8b2-f3ac-4cfc-9b30-a07a0aec753d
credentials-file: /etc/cloudflared/b123f8b2-f3ac-4cfc-9b30-a07a0aec753d.json
ingress:
  - hostname: api.specflow.dev
    service: http://localhost:8080
  - service: http_status:404
```

管理命令：

```bash
sudo systemctl start cloudflared
sudo systemctl stop cloudflared
sudo systemctl status cloudflared
sudo systemctl enable cloudflared   # 开机自启
```

### 公开访问 URL

| URL | 说明 |
|-----|------|
| `https://api.specflow.dev/actuator/health` | 健康检查 |
| `https://api.specflow.dev/swagger-ui/index.html` | Swagger UI |
| `https://api.specflow.dev/api/v1/` | API 根路径 |

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
