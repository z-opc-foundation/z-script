# z-script · Script Platform + Mock Server

> **L3 中间件 · Spring Boot Starter 一行集成** · **Maven Central**: `io.github.yuku123:z-script-*`
>
> 多 DSL 脚本执行（EL / Groovy / Lua / SQL / MOCK / API_BRIDGE）+ Mock 平台（端点、场景、用例、录制回放、故障注入）+ 自带 Web 控制台 + app/AK 鉴权与调用审计

[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java 8+](https://img.shields.io/badge/java-8%2B-orange)](https://openjdk.java.net/)

## 模块结构

```
z-script (aggregator, pom)            → io.github.yuku123:z-script
├─ z-script-core                      → 领域层：17 张表的实体 + MyBatis-Plus Mapper + Service
├─ z-script-engine                    → 引擎层：ScriptEngine / 各 DSL Sandbox / Mock 引擎 / 断言 / 录制回放
├─ z-script-web                       → Starter 层：18 个 Controller + API Key 拦截器 + 自动装配
├─ z-script-scene (aggregator, pom)
│   └─ z-script-scene-http            → 场景层：HTTP 链路编排（HttpChainExecutor + /api/scene-http）
└─ z-script-admin                     → 可执行控制台（jar 内嵌 SPA）；留 reactor，永不上 Central

_frontend (不入 reactor)              → 两层 npm 工程：SPA + 组件库
deploy   (不入 reactor)               → Mode 1/2/3 + K8s 部署模板
_doc                                    → 架构文档 / init.sql / 构建发布脚本
```

依赖方向：`admin → web → engine → core`，`scene-http → engine`。反向依赖不允许。

## 快速集成

```xml
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-script-web</artifactId>
    <version>1.0.0</version>
</dependency>
```

引入即自动装配（`META-INF/spring.factories` → `ZScriptWebAutoConfiguration`，
component-scan `com.zifang.z.script`）：引擎 Bean、Controller、`sqlSessionFactoryScript` 全部就位。

数据源走 z-boot-datasource-starter 的 **module 键**（不是 `spring.datasource.*`）：

```yaml
z:
  base:
    db:
      script:
        host: ${Z_BASE_DB_SCRIPT_HOST}
        port: ${Z_BASE_DB_SCRIPT_PORT:3306}
        database: ${Z_BASE_DB_SCRIPT_DATABASE:z_script}
        username: ${Z_BASE_DB_SCRIPT_USERNAME}
        password: ${Z_BASE_DB_SCRIPT_PASSWORD}
```

建库：`mysql -h<host> -uroot -p < _doc/002_deploy/init.sql`（幂等，17 张表 + demo 脚本/端点）。

## 鉴权：只有 app + AK 一种（应用是权限中心）

z-script **不挂 4A / SSO**，自带鉴权。外部调用方与它自己的控制台走同一套：请求头 `X-Api-Key`。

管理模型是 **应用为中心**：先建应用，AK 挂在应用下（一个应用可签发多把、随时重置吊销），
应用再挂一份「可访问脚本列表」—— 这批 Key 能调什么由应用说了算，Key 本身只负责认证。

```bash
# 1. 引导：唯一免鉴权端点（此时调用方手上还没有 Key）。
#    应用不存在会随签 Key 自动创建；secret 只在这一次返回
curl -X POST http://localhost:8086/script/api/script/api-key \
     -H 'Content-Type: application/json' \
     -d '{"appName":"z-qa","scope":"ALL","description":"QA 平台调用"}'

# 2. 之后一律带 AK
curl -H "X-Api-Key: zsk_live_xxx" http://localhost:8086/script/api/script/list

# 3. 治理：给应用配脚本列表（scope=SPECIFIC + scriptCode 数组）
curl -X POST 'http://localhost:8086/script/api/script/app/scripts?appCode=z-qa' \
     -H "X-Api-Key: zsk_live_xxx" -H 'Content-Type: application/json' \
     -d '{"scope":"SPECIFIC","scripts":["hello_world"]}'
```

禁用应用 → 名下所有 Key 一并 403 `APP_DISABLED`；scope=SPECIFIC 时只放行列表内的 scriptCode。

默认拒绝：`/api/**` 与历史运行时路由 `/run/**` 全量拦截（状态、过期、IP 白名单、scope、配额、可选 HMAC 签名逐条校验），
每次调用（含被拒的）异步落 `z_script_invoke_log`。不带 Key 的业务请求一律 401 —— 控制台也一样。

## 调用一个脚本

```bash
# Ad-hoc 执行（scriptCode 走 query，body 是脚本入参）
curl -X POST 'http://localhost:8086/script/api/script/run?scriptCode=hello_world' \
     -H "X-Api-Key: zsk_live_xxx" -H 'Content-Type: application/json' -d '{"name":"zifang"}'
# → {"data":"hello, zifang! @ z-script","success":true,"errorMessage":null,"durationMs":2}

# 已上线脚本的 HTTP 暴露路径
curl 'http://localhost:8086/script/api/script-run/hello_world?name=zifang' -H "X-Api-Key: zsk_live_xxx"

# Mock 端点由 path 命中，未命中回落 404 模板
curl http://localhost:8086/script/api/mock/hello -H "X-Api-Key: zsk_live_xxx"
```

DSL 类型只认 `EL | GROOVY | LUA | SQL | MOCK | API_BRIDGE`（`ScriptEngine` 分发）；
`EL` 是 SpEL，跑在 `SimpleEvaluationContext` 沙箱里，入参以 `#name` 引用。
Groovy/Lua 各有 Sandbox + 超时/策略限制（见 `z-script-engine/.../sandbox/`）。

## 控制台

`z-script-admin` 打出的 exec jar 已内嵌 SPA（构建期由 frontend-maven-plugin 产出 dist）：

```bash
java -jar z-script-admin/target/z-script-admin-1.0.0-exec.jar
# 浏览器打开 http://localhost:8086/script/ → 点右上角「签发 AK」→ 脚本 / Mock 端点两个 Tab
```

本地开发（前端热更新）见 [_frontend/README.md](_frontend/README.md)。

## 部署

| 模式 | 命令 | 形态 |
|---|---|---|
| Mode 1 合体 | `cd deploy && make dev WITH_DB=1` | 单容器（jar 内嵌前端） |
| Mode 2 分体 | `cd deploy && make split` | nginx 前端 + 后端 jar |
| Mode 3 集群 | `cd deploy && make cluster N=3` | nginx + N 后端 |
| K8s | `DB_HOST=... DB_PASSWORD=... INGRESS_DOMAIN=example.com bash deploy/bin/k8s-apply.sh` | Deployment/Service/Ingress |

细节、占位符与验证清单见 [deploy/README.md](deploy/README.md)。

## 构建与发布到 Central

```bash
bash _doc/003_script/build.sh            # reactor 构建（含测试）
bash _doc/003_script/deploy_maven_center.sh publish   # -Pcentral：flatten + GPG 签名 + 发布
```

只有 `core / engine / web / scene-http`（及聚合 pom）上传 Central；
`z-script-admin` 在 reactor 里 `maven.deploy.skip=true`，属架构规范原则 15（可运行应用不发中央仓库）。

## 凭证与安全

**本仓库不含任何明文凭证**：`application-{dev,local,prod}.yml` 已 gitignore，
Spring 配置 / compose / K8s manifest 里的连接信息全部由环境变量注入，
API Key 也不预置在 `init.sql` 中（由引导端点或控制台现场签发）。

参考规范：[z-opc-foundation-lead/008_组织规范/001_凭证管理规范.md](https://github.com/z-opc-foundation/z-opc-foundation-lead/blob/main/008_组织规范/001_凭证管理规范.md)

## 文档目录

- [`_doc/001_arch/00-overview.md`](_doc/001_arch/00-overview.md) — 模块边界、鉴权模型、孵化期间的整改记录
- [`_doc/002_deploy/z-script-admin.md`](_doc/002_deploy/z-script-admin.md) — 运行手册（启动 / 配置 / 鉴权 / API 速查 / 故障排查）
  · [`init.sql`](_doc/002_deploy/init.sql) — 17 表 + 幂等 seed
- [`_doc/003_script/`](_doc/003_script/) — 构建 / 打包 / 发布脚本
- [`_doc/004_skill/audit-incubation-1.0.0.md`](_doc/004_skill/audit-incubation-1.0.0.md) — 孵化入库审计（含 2 个已修的鉴权面缺陷）
- [`_frontend/README.md`](_frontend/README.md) — 前端两层工程与 vite base 约定
- [`deploy/README.md`](deploy/README.md) — 三种部署模式 + K8s
- [lead 仓技术架构规范](https://github.com/z-opc-foundation/z-opc-foundation-lead/blob/main/005_技术架构/) — 全 z-* 仓通用

## 关联

- 父组织：[z-opc-foundation](https://github.com/z-opc-foundation)
- L2 封装：[z-boot](https://github.com/z-opc-foundation/z-boot)（`z-boot-web-starter` / `z-boot-datasource-starter`）
- L1 工具：[z-util](https://github.com/z-opc-foundation/z-util)（`z-util-core/http/json`）

## 许可

MIT License — 详见 [LICENSE](LICENSE)
