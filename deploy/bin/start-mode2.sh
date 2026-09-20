#!/usr/bin/env bash
# Mode 2 分体启动：前端 nginx + 后端 jar 两个容器
# 详见 deploy/docker-compose.split.yml
#
#   bash bin/start-mode2.sh            # 连外部库（Z_BASE_DB_SCRIPT_*）
#   WITH_DB=1 bash bin/start-mode2.sh  # 连试用 MySQL 一起起

set -euo pipefail

cd "$(dirname "$0")/.."

WITH_DB_PROFILE=""
if [ "${WITH_DB:-0}" = "1" ]; then
    WITH_DB_PROFILE="--profile with-db"
fi

echo "=== 启动 Mode 2（分体）${WITH_DB_PROFILE:+ $WITH_DB_PROFILE} ==="
docker compose -f docker-compose.split.yml ${WITH_DB_PROFILE} up -d

echo ""
echo "✓ 启动完成。"
echo "  控制台：http://localhost/script/"
echo "  验证反代：curl -i http://localhost/script/api/script/list（无 X-Api-Key 应 401）"
echo "  后端本体：docker exec -it z-script-backend wget -qO- http://127.0.0.1:8086/script/actuator/health"
echo "  停止：docker compose -f docker-compose.split.yml ${WITH_DB_PROFILE} down"
