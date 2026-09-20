#!/usr/bin/env bash
# Mode 3 集群启动：1 前端 nginx + N 后端 jar（默认 N=2，位置参数覆盖）
# 详见 deploy/docker-compose.cluster.yml
#
#   bash bin/start-mode3.sh 5            # 5 个后端副本，连外部库
#   WITH_DB=1 bash bin/start-mode3.sh 3  # 连同试用 MySQL 起

set -euo pipefail

cd "$(dirname "$0")/.."

REPLICAS="${1:-2}"
COMPOSE="docker compose -f docker-compose.cluster.yml"

WITH_DB_PROFILE=""
if [ "${WITH_DB:-0}" = "1" ]; then
    WITH_DB_PROFILE="--profile with-db"
fi

echo "=== 启动 Mode 3（集群，backend replicas=${REPLICAS}）${WITH_DB_PROFILE:+ $WITH_DB_PROFILE} ==="
${COMPOSE} ${WITH_DB_PROFILE} up -d --scale z-script-backend="${REPLICAS}"

echo ""
echo "✓ 启动完成。"
${COMPOSE} ${WITH_DB_PROFILE} ps
echo ""
echo "  控制台：http://localhost/script/（nginx 轮询到各后端实例）"
echo "  验证反代：curl -i http://localhost/script/api/script/list（无 X-Api-Key 应 401）"
echo "  调整副本：bash bin/start-mode3.sh 5"
echo "  停止：${COMPOSE} ${WITH_DB_PROFILE} down"
