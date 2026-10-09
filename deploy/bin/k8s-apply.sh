#!/usr/bin/env bash
# k8s 一键部署：渲染占位符 → 建 Secret（不落 Git）→ apply → 等 rollout
#
# 必填环境变量：
#   INGRESS_DOMAIN   - 如 script.example.com
#   DB_HOST          - 数据库主机（集群内 Service 名或 RDS 地址）
#   DB_PASSWORD      - 数据库密码（只经命令行进 Secret；生产用 External Secrets 接管）
# 可选：
#   NAMESPACE        - 默认 z-script
#   DB_PORT          - 默认 3306
#   DB_DATABASE      - 默认 z_script
#   DB_USERNAME      - 默认 root
#   TLS_SECRET_NAME  - 默认 z-script-tls
#   OCI_REGISTRY     - 默认 ghcr.io/yuku123
#   IMAGE_VERSION    - 默认 1.0.0

set -euo pipefail

cd "$(dirname "$0")/.."

export NAMESPACE="${NAMESPACE:-z-script}"
export INGRESS_DOMAIN="${INGRESS_DOMAIN:?必须设置 INGRESS_DOMAIN（如 script.example.com）}"
export TLS_SECRET_NAME="${TLS_SECRET_NAME:-z-script-tls}"
export OCI_REGISTRY="${OCI_REGISTRY:-ghcr.io/yuku123}"
export IMAGE_VERSION="${IMAGE_VERSION:-1.0.0}"
export DB_HOST="${DB_HOST:?必须设置 DB_HOST（z_script 库所在 MySQL 地址）}"
export DB_PORT="${DB_PORT:-3306}"
export DB_DATABASE="${DB_DATABASE:-z_script}"
export DB_USERNAME="${DB_USERNAME:-root}"
export DB_PASSWORD="${DB_PASSWORD:?必须设置 DB_PASSWORD（本仓不放任何库密码，见 lead/008 凭证管理规范）}"

# 只替换已知占位符，避免 envsubst 顺手吃掉 yaml 里其它 $ 开头的字面量
ENVSUBST_VARS='${NAMESPACE} ${INGRESS_DOMAIN} ${TLS_SECRET_NAME} ${OCI_REGISTRY} ${IMAGE_VERSION} ${DB_HOST} ${DB_PORT} ${DB_DATABASE}'

RENDERED_DIR="$(mktemp -d)"
trap 'rm -rf "${RENDERED_DIR}"' EXIT

echo "=== 渲染占位符 → ${RENDERED_DIR} ==="
for f in k8s/*.yaml; do
    out="${RENDERED_DIR}/$(basename "$f")"
    envsubst "${ENVSUBST_VARS}" < "$f" > "$out"
    echo "  rendered: $(basename "$f")"
done

echo ""
echo "=== Namespace / RBAC 先落（Secret 与 Deployment 都要挂在它下面） ==="
kubectl apply -f "${RENDERED_DIR}/00-namespace.yaml"

echo ""
echo "=== 数据库凭证 Secret（现场创建，不落 Git） ==="
kubectl -n "${NAMESPACE}" create secret generic z-script-db-credentials \
    --from-literal=username="${DB_USERNAME}" \
    --from-literal=password="${DB_PASSWORD}" \
    --dry-run=client -o yaml | kubectl apply -f -

echo ""
echo "=== kubectl apply（configmap 先落，Deployment 才引用得到） ==="
for f in 06-configmap 01-deployment-backend 02-deployment-frontend \
         03-service-backend 04-service-frontend 05-ingress; do
    kubectl apply -f "${RENDERED_DIR}/${f}.yaml"
done

echo ""
echo "=== 等 rollout ==="
kubectl -n "${NAMESPACE}" rollout status deployment/z-script-backend --timeout=300s
kubectl -n "${NAMESPACE}" rollout status deployment/z-script-suit --timeout=180s

echo ""
echo "✓ 部署完成。控制台：https://script.${INGRESS_DOMAIN}/script/（Ingress host 见 05-ingress.yaml）"
echo "  首次使用：在控制台点「签发 AK」，或 curl -X POST https://script.${INGRESS_DOMAIN}/script/api/script/api-key -d '{\"appName\":\"my-app\"}'"
