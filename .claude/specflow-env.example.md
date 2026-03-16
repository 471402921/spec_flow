# SpecFlow 部署环境配置

> 使用方法: `cp specflow-env.example.md specflow-env.md` 并填入你的实际值
> `specflow-env.md` 已在 .gitignore 中，不会被提交到仓库

## Deploy

| 配置项 | 值 | 说明 |
|--------|-----|------|
| ssh_alias | `your-server` | SSH config 中的 Host 别名 |
| service_path | `/srv/specflow-service` | 服务器上的项目根目录 |
| api_port | `8080` | API 服务端口 |

## Server

| 配置项 | 值 | 说明 |
|--------|-----|------|
| ip | `` | 服务器公网 IP（可选，仅供参考） |
| ssh_user | `ubuntu` | SSH 登录用户名 |
| ssh_key | `~/.ssh/your-key.pem` | SSH 私钥路径 |

## Database

| 配置项 | 值 | 说明 |
|--------|-----|------|
| name | `specflow` | PostgreSQL 数据库名 |
| user | `specflow` | PostgreSQL 用户名 |
| container | `specflow-postgres` | PostgreSQL Docker 容器名 |

## Containers

| 配置项 | 值 | 说明 |
|--------|-----|------|
| prefix | `specflow` | Docker 容器名前缀 |

容器命名规则: `{prefix}-postgres`, `{prefix}-redis`, `{prefix}-api`
