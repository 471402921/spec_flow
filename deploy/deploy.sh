#!/bin/bash
# =============================================
# SpecFlow Service 部署脚本
# 用途: 在 home-node 上快速部署应用
# =============================================

set -e  # 遇到错误立即退出

echo "🚀 开始部署 SpecFlow Service..."

# 1. 拉取最新代码
echo "📥 拉取最新代码..."
git pull origin main

# 2. 停止现有服务
echo "🛑 停止现有服务..."
docker compose down

# 3. 构建并启动服务
echo "🔨 构建并启动服务..."
docker compose up -d --build

# 4. 等待服务启动
echo "⏳ 等待服务启动（15秒）..."
sleep 15

# 5. 健康检查
echo "🏥 执行健康检查..."
HEALTH_STATUS=$(curl -s http://localhost:8080/actuator/health | grep -o '"status":"UP"' || echo "")

if [ -n "$HEALTH_STATUS" ]; then
    echo "✅ 部署成功！健康检查通过"
    echo ""
    echo "📊 服务状态："
    docker compose ps
    echo ""
    echo "🌐 访问地址："
    echo "   - 本地: http://localhost:8080/swagger-ui/index.html"
    echo "   - 公网: https://api.specflow.dev/swagger-ui/index.html"
else
    echo "❌ 部署失败：健康检查未通过"
    echo ""
    echo "查看日志："
    docker compose logs --tail=50 api
    exit 1
fi
