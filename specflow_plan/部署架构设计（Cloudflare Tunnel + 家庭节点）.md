# 部署架构设计（新版：Cloudflare Tunnel + 家里运行节点 + Mac/SSH 运维）

> 目标：在不购买 VPS 的前提下，实现：
> - 公网 HTTPS 访问（支持 WebSocket）
> - 运维通道与业务通道分离（安全、可控）
> - Mac 上用 ClaudeCode 通过 SSH 完成全部部署与运维
> - 后续可平滑迁移到 VPS / 云

---

## 1. 总体架构概览

### 1.1 逻辑拓扑
公网用户 / 第三方平台
│ HTTPS (80/443)
▼
Cloudflare（DNS + WAF + TLS 终止）
│ Cloudflare Tunnel（反向隧道）
▼
家里 Windows 主机（不暴露公网入站端口）
▼
WSL2 / Ubuntu（运行容器与隧道客户端）
▼
Docker Compose（API / Worker / DB / Redis 等）

### 1.2 运维通道（与业务通道隔离）
Mac（开发主机 + ClaudeCode）
│ SSH（`ssh home-node`）
▼
家里 Windows/WSL（运维执行端）

> **标准运维入口**：`ssh home-node`（已配置 SSH 别名，可直接访问）
> 业务流量走 Cloudflare Tunnel；运维流量走 SSH。
> 两条通道彼此独立，避免"把管理口暴露到公网"。
> ToDesk 仅作为兜底控制台，不作为日常运维入口。

---

## 2. 组件职责划分（谁负责什么）

### 2.1 Cloudflare（公网入口层）
- DNS 托管（权威 DNS）
- HTTPS 证书与 TLS 终止
- 基础安全防护（WAF/限流可选）
- Cloudflare Tunnel 路由（hostname → origin service）

### 2.2 家里 Windows 主机（宿主层）
- 仅作为运行宿主与远程接入环境
- 保证：
  - 不休眠
  - 不自动重启（或重启可自恢复）
- ToDesk 仅作为兜底控制台，不作为日常运维入口

### 2.3 WSL2 / Ubuntu（执行与运行层）
- Docker Engine / Docker Compose（在 WSL 内运行，已部署）
- cloudflared（Tunnel 客户端，以 systemd 服务常驻）
- SSH Server（用于运维执行）
- 运行目录：`/srv/soulpal-service`
- 用户：`ssccddjjjj`

### 2.4 Docker Compose（应用运行层）
- API（HTTP + OpenAPI；WebSocket 可选）
- Worker（异步任务/队列消费）
- PostgreSQL（开发/测试阶段可同机）
- Redis（缓存/队列/会话）
- 可选：日志采集/监控（后续再上）

---

## 3. 域名与访问策略

### 3.1 域名规划（建议）
- `api.example.com`：对外 API（业务入口）
- `ws.example.com`：WebSocket（可选，若要独立域名）
- `admin.example.com`：管理后台（不建议公网直开；可用 Cloudflare Access 保护）

> 说明：可以先只开 `api` 一个子域名，后续再拆分。

### 3.2 Cloudflare Tunnel 路由策略
- 每个 hostname 映射到 WSL 内的某个本地端口，例如：
  - `api.soulpal.me` → `http://localhost:8080`
  - `ws.soulpal.me`  → `http://localhost:8080`（同服务同端口也可）
- 默认兜底为 404（避免误暴露）

## 3.3 实际部署配置（已实施）
| 配置项 | 值 |
|--------|-----|
| 域名 | `soulpal.me` |
| Tunnel 名称 | `home-dev` |
| Tunnel ID | `b123f8b2-f3ac-4cfc-9b30-a07a0aec753d` |
| DNS 记录 | `api.soulpal.me` CNAME → `b123f8b2-f3ac-4cfc-9b30-a07a0aec753d.cfargotunnel.com` |
| 本地服务 | `http://localhost:8080`（Spring Boot 默认端口）|
| 配置文件 | `/etc/cloudflared/config.yml` |
| 凭证文件 | `/etc/cloudflared/b123f8b2-f3ac-4cfc-9b30-a07a0aec753d.json` |
| **Tailscale IP** | `100.104.158.31`（WSL2 独立安装 tailscale）|
| **SSH 配置** | `~/.ssh/config` 中 `HostName` 指向 Tailscale IP |

### 验证方式
- 浏览器访问 `https://api.soulpal.me/cdn-cgi/trace` 可查看 Cloudflare 连接信息
- 返回 `colo=SJC` 等字段表示 Tunnel 连接正常

## 3.4 Docker Compose 部署配置（已实施）

| 配置项 | 值 |
|--------|-----|
| 项目路径 | `/srv/soulpal-service` |
| Compose 文件 | `deploy/docker-compose.yml` |
| 构建文件 | `deploy/Dockerfile`（multi-stage: JDK 21 build + JRE 21 runtime）|
| 环境变量 | `deploy/.env`（基于 `.env.example` 创建，有默认值可省略）|

**容器清单**:

| 容器 | 镜像 | 端口 | 状态 |
|------|------|------|------|
| soulpal-postgres | postgres:16 | 5432 | ✅ Healthy |
| soulpal-redis | redis:7-alpine | 6379 | ✅ Healthy |
| soulpal-api | 本地构建 | 8080 | ✅ Running |

**Docker 镜像加速器**（中国网络必需）:
```json
// /etc/docker/daemon.json
{
  "registry-mirrors": [
    "https://docker.1ms.run",
    "https://docker.xuanyuan.me"
  ]
}
```

---

## 4. 网络与安全边界

### 4.1 公网入站端口策略
- 家里路由器/Windows：不做端口转发（0 入站）
- 公网只暴露在 Cloudflare
- origin（家里服务）通过 Tunnel 反向出站连接 Cloudflare

### 4.2 运维 SSH 策略（Tailscale 网络）

**网络架构**：
- **Windows 主机**：运行 Tailscale 客户端（IP: `100.66.161.47`）
- **WSL2**：独立运行 Tailscale（IP: `100.104.158.31`），**不能依赖 Windows 主机的 Tailscale 连接**
- **Mac 开发机**：通过 Tailscale 连接到 WSL2 的 IP

**SSH 配置**（`~/.ssh/config`）：
```
Host home-node
  HostName 100.104.158.31  # WSL2 Tailscale IP
  User ssccddjjjj
  IdentityFile ~/.ssh/id_ed25519
  ServerAliveInterval 30
```

**WSL2 Tailscale 安装与启动**：
```bash
# 1. 安装 Tailscale
curl -fsSL https://tailscale.com/install.sh | sh

# 2. 启动 tailscaled（WSL2 无 systemd 时）
sudo nohup tailscaled > /tmp/tailscaled.log 2>&1 &

# 3. 登录
sudo tailscale up

# 4. 查看 IP
tailscale ip -4
```

**注意事项**：
- WSL2 与 Windows 主机使用独立的 Tailscale IP
- WSL2 重启后需手动启动 tailscaled（或启用 systemd）
- ToDesk 作为兜底控制台，当 Tailscale 网络异常时使用

### 4.3 数据与密钥策略
- `.env` 只存在运行节点（不提交仓库）
- 数据库/Redis 不暴露公网
- 备份落本地 HDD + 可选云备份（后续）

---

## 5. 环境划分（简单但可扩展）

### 5.1 早期推荐（单机多环境最小化）
- `dev`：Mac 本地（可选）
- `staging`：家里运行节点（对外可访问，用于联调/内测）
- `prod`：未来迁移到 VPS/云（同一套 Compose/镜像策略迁移）

> 说明：当前阶段可把家里节点当 staging/内测环境使用。

### 5.2 配置隔离方式
- `docker-compose.yml`（公共）
- `docker-compose.staging.yml`（覆盖项：端口、资源、日志）
- `.env.staging`（仅本机）
- 迁移到 prod 时沿用同样结构

---

## 6. 部署与发布流程（Mac 主控）

### 6.1 "单机发布"最小流程（已验证可用）
1. Mac：开发完成并 push 到 GitHub
2. Mac（ClaudeCode 通过 `ssh home-node`）在远端执行：
   ```bash
   ssh home-node "cd /srv/soulpal-service && git pull origin main && cd deploy && docker compose up -d --build"
   ```
3. 验证：
   ```bash
   # 本地验证
   ssh home-node "curl http://localhost:8080/actuator/health"
   # 公网验证
   curl https://api.soulpal.me/actuator/health
   ```
4. Cloudflare Tunnel 持续在线，对外域名不变

### 6.2 发布关键点
- 发布操作只需要 SSH，不依赖 ToDesk
- Tunnel 与应用解耦：应用重启不影响域名体系
- SSH 非交互式 sudo 使用 `echo 'password' | sudo -S` 模式（无 TTY）
- 首次需确保 Docker daemon 已启动：`sudo service docker start`

---

## 7. 可用性与自恢复（不复杂但要有效）

### 7.1 常驻服务
- `cloudflared` 以 systemd 服务常驻
- Docker Compose 使用 `restart: unless-stopped`

### 7.2 重启恢复策略
- Windows 重启后：
  - WSL 自动启动（按需配置）
  - cloudflared 自动启动
  - docker compose 自动拉起容器（由 restart 策略）

> 目标：家里机器重启后，无需人工介入即可恢复公网访问。

### 7.3 WSL2 systemd 注意事项
- 在 `/etc/wsl.conf` 中添加 `[boot] systemd=true` 配置后，**需要重启 WSL2** 才能生效：
  ```bash
  # Windows PowerShell 中执行
  wsl --shutdown
  # 重新打开 WSL2
  ```
- 在 systemd 生效前，可使用 `nohup` 临时启动 cloudflared：
  ```bash
  nohup cloudflared tunnel run home-dev > /tmp/cloudflared.log 2>&1 &
  ```

### 7.4 文件权限配置
- cloudflared 配置文件和凭证文件需要设置正确的权限：
  ```bash
  sudo chmod 644 /etc/cloudflared/config.yml
  sudo chmod 644 /etc/cloudflared/<TUNNEL_ID>.json
  ```
- 权限过严（如 600）可能导致 cloudflared 无法读取文件

---

## 8. 备份与恢复（最小基线）

### 8.1 备份对象
- PostgreSQL：逻辑备份（dump）
- `.env`：加密备份（仅你可解）
- 关键配置：
  - cloudflared config
  - docker compose 文件
  - 运维脚本（仓库管理）

### 8.2 备份位置
- 本机 HDD（冷备）
- 可选：云盘/对象存储（后续）

---

## 9. 迁移路线（从家里节点到 VPS/云，一步到位）

### 9.1 迁移原则
- 公网入口（域名/HTTPS）尽量保持不变
- 只替换 origin：
  - 从 `家里节点` → `VPS/云`

### 9.2 迁移方式（两种）
- 方式 A：Tunnel 继续使用，改 `service` 指向 VPS
- 方式 B：VPS 直接对外提供服务，Cloudflare 作为 DNS/CDN

> 结论：当前架构天然具备“可迁移性”，不会推翻重来。

---

## 10. 最终总结（本方案的核心价值）

- 公网访问：Cloudflare 负责（稳定、安全、HTTPS）
- 家里机器：仅承担运行与验证（不暴露公网入站）
- 运维控制：Mac + SSH（`ssh home-node`）+ ClaudeCode（效率最高）
- 未来迁移：平滑升级到 VPS/云（架构不推翻）

---

## 11. 部署实施记录

### 已完成（2026-02-06）
- ✅ Cloudflare Tunnel 配置完成（`api.soulpal.me` → `localhost:8080`）
- ✅ Docker Compose 部署完成（postgres + redis + api）
- ✅ 公网 HTTPS 访问正常（`https://api.soulpal.me/actuator/health`）
- ✅ GitHub SSH 认证配置完成（home-node → GitHub）

### 部署中遇到的关键问题
1. **Docker Hub 国内不可用** → 配置镜像加速器 `/etc/docker/daemon.json`
2. **Git 空目录导致 Docker 构建失败** → 添加 `.gitkeep` + 改进 Dockerfile COPY 策略
3. **WSL2 Docker daemon 不自动启动** → 需手动 `sudo service docker start`
4. **SSH 无 TTY 导致 sudo 失败** → 使用 `echo 'pw' | sudo -S` 模式
