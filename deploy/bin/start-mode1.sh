#!/usr/bin/env bash
# Mode 1 合体启动：单容器，后端 jar 内嵌前端
# 详见 deploy/docker-compose.yml
#
#   bash bin/start-mode1.sh            # 只起后端，连 Z_BASE_DB_SCRIPT_HOST 指向的库
#   WITH_DB=1 bash bin/start-mode1.sh  # 一起起试用 MySQL（首建自动跑 _doc/002_deploy/init.sql）

set -euo pipefail

cd "$(dirname "$0")/.."

# 值为固定字面量，故意不加引号让 shell 按词切开传给 compose
WITH_DB_PROFILE=""
if [ "${WITH_DB:-0}" = "1" ]; then
    WITH_DB_PROFILE="--profile with-db"
fi

echo "=== 启动 Mode 1（合体）${WITH_DB_PROFILE:+ $WITH_DB_PROFILE} ==="
docker compose ${WITH_DB_PROFILE} up -d

echo ""
echo "✓ 启动完成。"
echo "  控制台：http://localhost:8086/script/（先点「签发 AK」）"
echo "  健康检查：curl http://localhost:8086/script/actuator/health"
echo "  日志：docker compose logs -f z-script-admin"
echo "  停止：docker compose ${WITH_DB_PROFILE} down"
