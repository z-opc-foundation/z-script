# deploy/

> z-script 仓的**部署资源目录**。按规范 §4 三种模式组织：
> Mode 1（合体）、Mode 2（分体）、Mode 3（集群）+ K8s 一键部署。

## 目录结构

```
deploy/
├── docker-compose.yml              # Mode 1：合体（默认，最快上手；含 with-db profile）
├── docker-compose.split.yml        # Mode 2：分体（前端 nginx + 后端 jar）
├── docker-compose.cluster.yml      # Mode 3：集群（1 前端 + N 后端）
├── Dockerfile.backend              # 后端 jar 镜像（Mode 1/2/3 共用，JRE 8）
├── Dockerfile.frontend             # 前端 nginx 镜像（Mode 2/3 用，dist → html/script）
├── nginx.conf.template             # nginx 模板（envsubst 注入 BACKEND_SERVICE）
├── Makefile                        # make dev/split/cluster/build/push/k8s-apply/ps/logs/down/clean
├── bin/
│   ├── build-images.sh             # 一键构建两个镜像（--push / --no-cache）
│   ├── start-mode1.sh              # Mode 1 启动（WITH_DB=1 连同试用库）
│   ├── start-mode2.sh              # Mode 2 启动（同上）
│   ├── start-mode3.sh [N]          # Mode 3 启动（默认 N=2）
│   └── k8s-apply.sh                # K8s 一键部署（渲染 → 建 Secret → apply → 等 rollout）
├── env/
│   └── .env.example                # 环境变量模板（.env 自己创建，已被 gitignore）
└── k8s/
    ├── 00-namespace.yaml           # Namespace + ServiceAccount + Role + RoleBinding
    ├── 01-deployment-backend.yaml  # 后端 Deployment（replicas=2，env 走 ConfigMap/Secret）
    ├── 02-deployment-frontend.yaml # 前端 Deployment（replicas=1，注入 BACKEND_SERVICE）
    ├── 03-service-backend.yaml     # 后端 ClusterIP Service
    ├── 04-service-frontend.yaml    # 前端 ClusterIP Service
    ├── 05-ingress.yaml             # Ingress（HTTPS + 自动证书）
    └── 06-configmap.yaml           # 非敏感 DB 配置（Secret 不落 Git，见下）
```

## 三种模式对比

| 模式 | 启动命令 | 容器数 | 访问地址 | 适用场景 |
|---|---|---|---|---|
| Mode 1 合体 | `make dev` | 1（+1 可选试用库） | http://localhost:8086/script/ | 试用 / PoC / 个人开发 |
| Mode 2 分体 | `make split` | 2（+1 可选试用库） | http://localhost/script/ | 前端频繁迭代 / CDN |
| Mode 3 集群 | `make cluster N=3` | 1+N（+1 可选试用库） | http://localhost/script/ | 生产 / 多副本高可用 |

## 关键约定（三种模式一致）

| 项 | 值 |
|---|---|
| 后端端口 | `8086`（`SERVER_PORT` 可覆盖） |
| context-path | `/script`，与 `_frontend/z-script-suit` 的 vite `base` 严格一致 |
| 控制台 | `http://<host>:8086/script/` |
| 健康检查 | `/script/actuator/health`（只暴露 `health,info`，`show-details: never`） |
| 业务 API 前缀 | `/script/api/**`（仓内所有 Controller 都在 `/api` 下） |
| 鉴权 | **只有 app + AK**：请求头 `X-Api-Key`，默认全拒，仅 `POST /api/script/api-key` 免鉴权 |
| DB 配置键 | `z.base.db.script.*` ← env `Z_BASE_DB_SCRIPT_{HOST,PORT,DATABASE,USERNAME,PASSWORD}` |

> ⚠️ 不是 `spring.datasource.*` / `SPRING_DATASOURCE_*`。z-boot-datasource-starter 的
> `ModuleDataSourceTemplate` 只认 `z.base.db.<module>.*`，设错键会静默回落到 `localhost/root/空密码`。

## 快速上手

```bash
cd deploy

# 1a. 连已有 MySQL（先自己把库建好，见下一节）
cp env/.env.example .env   # 填 Z_BASE_DB_SCRIPT_HOST / PASSWORD
make dev

# 1b. 或者让 compose 顺带起一个试用 MySQL（首建自动跑 init.sql）
make dev WITH_DB=1

# 2. 控制台：http://localhost:8086/script/
#    没有 Key 时列表会报 MISSING_API_KEY —— 点右上角「签发 AK」（或自己 curl 创建）
# 3. 日志 / 停止
make logs
make down          # 已带 --profile with-db，试用库会一起停
```

## 建库与 API Key

```sql
-- 16 张表 + demo 数据，幂等可重复执行；刻意不含任何 API Key
mysql -h127.0.0.1 -uroot -p < ../_doc/002_deploy/init.sql
```

```bash
# 唯一免鉴权的引导端点；plainSecret 只在响应里出现这一次
curl -X POST http://localhost:8086/script/api/script/api-key \
     -H 'Content-Type: application/json' \
     -d '{"appName":"my-app","scope":"ALL","description":"本地试用"}'

# 之后所有调用都带 X-Api-Key
curl -H "X-Api-Key: zsk_live_xxx" http://localhost:8086/script/api/script/list
```

## 构建 + 推送镜像

```bash
make build                 # 本地构建两个镜像（不 push）
make push                  # 构建并 push 到 ${OCI_REGISTRY}（默认 ghcr.io/yuku123）
IMAGE_VERSION=1.0.1 make build
```

`bin/build-images.sh` 第一步会 `mvn -pl z-script-admin -am package`，
即「先出 exec jar（内嵌前端 dist），再打镜像」。

## K8s 部署

```bash
cd deploy
NAMESPACE=z-script \
INGRESS_DOMAIN=example.com \
DB_HOST=mysql-alpha DB_DATABASE=z_script DB_USERNAME=root DB_PASSWORD='<从密钥系统取>' \
bash bin/k8s-apply.sh
```

前置：
- 已有 k8s/k3s 集群 + kubectl 已配置，镜像已 push 到 `OCI_REGISTRY`（默认 `ghcr.io/yuku123`）
- `DB_PASSWORD` 必填 —— 脚本现场 `kubectl create secret` 建 `z-script-db-credentials`，
  仓库里**没有**任何 Secret 模板（见 lead/008_组织规范/001_凭证管理规范.md）

占位符（`${XXX}` 风格，envsubst 白名单渲染）：

| 变量 | 默认 | 说明 |
|---|---|---|
| `NAMESPACE` | `z-script` | 命名空间 |
| `INGRESS_DOMAIN` | 必填 | 最终 host 为 `script.<INGRESS_DOMAIN>` |
| `TLS_SECRET_NAME` | `z-script-tls` | Ingress 引用证书 Secret |
| `OCI_REGISTRY` | `ghcr.io/yuku123` | 镜像仓库前缀 |
| `IMAGE_VERSION` | `1.0.0` | 镜像 tag |
| `DB_HOST` / `DB_PORT` / `DB_DATABASE` | 必填 / 3306 / `z_script` | 落 `06-configmap.yaml` |
| `DB_USERNAME` / `DB_PASSWORD` | root / 必填 | 落 Secret |

## 占位符渲染

`k8s/*.yaml` 用 `${XXX}`（envsubst 风格），`bin/k8s-apply.sh` 渲染到临时目录后 apply。
手动渲染查看：

```bash
for f in k8s/*.yaml; do
    NAMESPACE=z-script INGRESS_DOMAIN=example.com OCI_REGISTRY=ghcr.io/yuku123 \
    IMAGE_VERSION=1.0.0 TLS_SECRET_NAME=z-script-tls \
    DB_HOST=mysql-alpha DB_PORT=3306 DB_DATABASE=z_script \
    envsubst '${NAMESPACE} ${INGRESS_DOMAIN} ${TLS_SECRET_NAME} ${OCI_REGISTRY} ${IMAGE_VERSION} ${DB_HOST} ${DB_PORT} ${DB_DATABASE}' \
      < "$f" > "/tmp/$(basename "$f")"
done
```

## 验证清单

```bash
# Mode 1
curl -s http://localhost:8086/script/actuator/health          # {"status":"UP"}
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8086/script/          # 200（内嵌 SPA）
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8086/script/api/script/list   # 401（未带 AK）

# Mode 2 / 3（走 nginx）
curl -s -o /dev/null -w '%{http_code}\n' http://localhost/script/               # 200
curl -s -o /dev/null -w '%{http_code}\n' http://localhost/script/api/script/list # 401
curl -s http://localhost/healthz                                               # ok（nginx 自身）
```

## 与 lead 部署文档的关系

按规范 §4.5，本目录是**单中间件**部署模板；lead/006_部署方案 是**全集**部署编排。
本目录落地后，lead/006 应反向引用本仓 `deploy/` 而非重复维护 yaml。

## 许可

MIT
