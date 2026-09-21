# z-script 部署与运行手册

> 自包含: 本目录 (`z-script-admin/`) + z-script-engine + z-script-web + z-script-core
> 即可对外提供「脚本执行 + Mock 服务」HTTP API 与自带控制台。
> 分体/集群形态见 [deploy/README.md](../../deploy/README.md)。

## 端口与路径

| 协议   | 端口       | 备注                                              |
|------|----------|-------------------------------------------------|
| HTTP | 8086     | `SERVER_PORT` 可覆盖；context-path 固定 `/script` |

只有一个端口。控制台、业务 API、OpenAPI 文档、actuator 全在 `/script` 前缀下（同源，无 CORS）。

MCP 不监听端口：`ScriptMcpAdapter` 按 bean 名反射拿 `mcpRegistry`（由 z-agent-mcp-center 提供），
把它注册到宿主的 MCP 注册表里。单独跑 admin 时启动日志会输出
`ScriptMcpAdapter bootstrap: McpRegistry not present, skipping` —— 这是预期行为，
`exposeAs=MCP/BOTH` 的脚本此时只落库、不对外暴露。

## 启动

### 前置: JDK 8 运行，JDK 17 编译

`maven.compiler.source/target=1.8`，产物字节码跑在 JDK 8 上。编译请用 JDK 17
（更高版本 JDK 下 Lombok/插件链会出问题）：

```bash
export JAVA_HOME_8=/Users/zifang/Library/Java/JavaVirtualMachines/corretto-1.8.0_492/Contents/Home
export JAVA_HOME_17=/Users/zifang/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home
```

### 本地开发（有 node，前端一起打进 jar）

```bash
# 1. 建库 + 建表 + 种子数据（17 张表 + 3 条 seed）
mysql -h127.0.0.1 -P3306 -uroot -e "CREATE DATABASE IF NOT EXISTS z_script CHARACTER SET utf8mb4"
mysql -h127.0.0.1 -P3306 -uroot z_script < _doc/002_deploy/init.sql

# 2. 打包（frontend-maven-plugin 自装 node 18.17.0 → 组件层 build → vite build → 拷进 static/）
JAVA_HOME=$JAVA_HOME_17 mvn -pl z-script-admin -am package

# 3. 用 JDK 8 跑
$JAVA_HOME_8/bin/java -jar z-script-admin/target/z-script-admin-1.0.0-exec.jar \
    --z.base.db.script.host=127.0.0.1 --z.base.db.script.username=root
```

启动 ~4.4s，日志 31 行、0 WARN、0 ERROR。控制台 <http://localhost:8086/script/>。

### 本机无 node / CI 只发库

```bash
JAVA_HOME=$JAVA_HOME_17 mvn -pl z-script-admin -am package -Dfrontend.skip=true
```

跳过后 jar 能出，但 `static/` 为空 → 访问 `/script/` 404，API 不受影响。

### 没有 MySQL 时先起来试用

```bash
cd deploy && docker compose --profile with-db up -d   # 库起在 127.0.0.1:13306，自动跑 init.sql
# 后端指向它：
Z_BASE_DB_SCRIPT_PORT=13306 java -jar ../z-script-admin/target/z-script-admin-1.0.0-exec.jar
```

试用卷之外不要用空密码；正式环境的密码走 env / K8s Secret（见 deploy/README.md）。

## 配置项

| 配置                               | 默认          | 说明                                              |
|----------------------------------|-------------|-------------------------------------------------|
| `server.port` / `SERVER_PORT`    | 8086        | HTTP 端口                                         |
| `server.servlet.context-path`    | `/script`   | **不可随意改**：必须等于 `_frontend/.../vite.config.js` 的 `base` |
| `z.base.db.script.host`          | `localhost` | env `Z_BASE_DB_SCRIPT_HOST`                     |
| `z.base.db.script.port`          | 3306        | env `Z_BASE_DB_SCRIPT_PORT`                     |
| `z.base.db.script.database`      | `z_script`  | env `Z_BASE_DB_SCRIPT_DATABASE`                 |
| `z.base.db.script.username`      | `root`      | env `Z_BASE_DB_SCRIPT_USERNAME`                 |
| `z.base.db.script.password`      | 空           | env `Z_BASE_DB_SCRIPT_PASSWORD`，生产必须走 Secret |
| `spring.profiles.active=dev`     | -           | 只把 `com.zifang.z.script` 日志开到 DEBUG，鉴权不放宽      |
| `management.endpoints.web.exposure.include` | `health,info` | 探针 `/actuator/health/{liveness,readiness}`（`probes.enabled=true`） |
| `-Dz.script.api-key.enabled=false` | (默认 true) | **只给本机排障用**：整体关掉 X-Api-Key 校验，生产禁用         |

数据源由 `z-boot-datasource-starter` 的 `ModuleDataSourceTemplate` 按模块前缀读取，
所以键是 `z.base.db.script.*` 而 **不是** `spring.datasource.*` —— 写错不报错，
会静默回落到 `localhost/root/空密码/z_script`，是孵化前最难查的一类故障。

## 鉴权：只有 app + AK（应用是权限中心）

z-script 不接 z-ctc 4A / SSO，也没有登录页。控制台与被调用方走同一套 `X-Api-Key`。
管理模型是**应用为中心**：AK 挂在应用下（一个应用多把 Key，可随时重置吊销），
应用挂「可访问脚本列表」—— Key 只认证，能调什么由应用决定。

引导（唯一免鉴权端点，`ZScriptWebMvcConfig` 的精确 exclude；应用不存在会随签 Key 自动创建）：

```bash
curl -s -X POST http://localhost:8086/script/api/script/api-key \
     -H 'Content-Type: application/json' \
     -d '{"appName":"my-app","scope":"ALL","description":"incubation"}' | jq
# data.apiKey / data.apiSecret —— secret 明文只返回这一次，库里只存 SHA256
```

调用：

```bash
curl -s "http://localhost:8086/script/api/script/list" -H "X-Api-Key: $KEY"
```

`ApiKeyAuthInterceptor` 校验链与错误码：

| 序   | 校验          | 失败码               | HTTP |
|-----|-------------|-------------------|------|
| 1-2 | 头存在 / Key 存在 | `MISSING_API_KEY` / `INVALID_API_KEY` | 401 |
| 3   | 启用状态        | `API_KEY_DISABLED` | 401  |
| 4   | 过期时间        | `API_KEY_EXPIRED`  | 401  |
| 5   | IP 白名单（支持 CIDR） | `IP_NOT_ALLOWED`   | 403  |
| 6   | 应用状态 + scope | `APP_DISABLED` / `SCOPE_NOT_ALLOWED` | 403  |
| 7   | 配额          | `QUOTA_EXCEEDED`（带 `Retry-After`） | 429 |
| 8   | 签名（可选）      | `INVALID_SIGNATURE` | 401  |

- **默认拒绝**：拦 `/api/**` 与历史运行时路由 `/run/**`，新增 Controller 只要落在 `/api` 下就自动受保护。
- **应用决定权限**：Key → `app_id` → `z_script_app`；`scope=ALL` 放行全部脚本，
  `SPECIFIC` 只放行 `allowed_scripts` 里的 scriptCode；应用 `status=0` 时名下所有 Key 403 `APP_DISABLED`。
  存量 Key 找不到应用时回退到 Key 自己的 scope 列（兼容 `init.sql` 升级前的库）。
- 管理面端点解析不出 scriptCode，`SPECIFIC` 的应用调不到 `list/publish` —— 给控制台用的应用保持 `ALL`。
- 签名：同时带 `X-Timestamp`（毫秒，±5min 容差）和 `X-Signature` 才校验。
  串为 `method + "\n" + path + "\n" + timestamp + "\n" + body`，HMAC-SHA256 key 是 `api_secret_hash`
  （不是明文 secret）。⚠️ 服务端 `readBody()` 目前恒返回 `""`，所以带请求体的 POST 无法验签通过 ——
  现阶段只对 GET / 无体请求可用。
- 每次调用（含被拒的）异步写 `z_script_invoke_log`，写失败只吞不抛。

## API 速查

| Method       | Path（省略 `/script` 前缀）                    | 用途                                  |
|--------------|--------------------------------------------|-------------------------------------|
| `POST`       | `/api/script/api-key`                      | 签发 Key（唯一免鉴权；应用不存在会自动创建）           |
| `GET`        | `/api/script/api-key/page`                 | Key 分页                              |
| `POST`       | `/api/script/api-key/{id}/reset-secret`    | 重置 secret（明文同样只返回一次）                 |
| `GET`        | `/api/script/app/list`                     | 应用列表                                |
| `POST`       | `/api/script/app`                          | 创建应用（appCode 必填）                     |
| `PUT`        | `/api/script/app?appCode=`                 | 更新应用 / 启用禁用（status）                  |
| `POST`       | `/api/script/app/scripts?appCode=`         | 配置应用的可访问脚本列表                         |
| `POST`       | `/api/script/app/{appCode}/enable\|disable` | 启用/禁用应用（名下 Key 一并 403/恢复）           |
| `DELETE`     | `/api/script/app?appCode=`                 | 删除应用（名下还有 Key 时拒绝）                   |
| `GET`        | `/api/script/list?dslType=\|exposeAs=`   | 列表（两参二选一，同时给以 dslType 为准）        |
| `GET`        | `/api/script/byCode?scriptCode=`           | 详情                                  |
| `POST`       | `/api/script`                              | 创建（`exposeAs` 含 HTTP 时自动生成 httpPath） |
| `PUT`        | `/api/script?scriptCode=`                  | 更新                                  |
| `DELETE`     | `/api/script?scriptCode=`                  | 删除                                  |
| `POST`       | `/api/script/run?scriptCode=`              | Ad-hoc 执行（控制台「执行」按钮）                 |
| `POST`       | `/api/script/publish?scriptCode=&exposeAs=HTTP\|MCP\|BOTH` | 发布，`status`→1             |
| `POST`       | `/api/script/unpublish?scriptCode=`        | 取消发布，`status`→0、`exposeAs`→NONE      |
| `POST`       | `/api/script/import-curl` `/import-openapi` `/import` `/copy` | 批量入口                     |
| `ANY`        | `/api/script-run/{scriptCode}/**`          | 调用已发布为 HTTP 的脚本（推荐入口）                 |
| `GET/POST`   | `/run/{scriptCode}`                        | 同一能力的历史入口，返回 `Result` 包装             |
| `ANY`        | `/api/mock/**`、`/api/mock/{envCode}/**`    | Mock 分发（`X-Mock-Env` 可指定环境）          |
| `GET`        | `/api/mock-platform/endpoints/list`        | Mock 端点列表（控制台第二个页签）                  |
| `GET`        | `/api/script/invoke-log/page`              | 调用日志；`/clean` 清理                     |
| `GET`        | `/api/script/quota`、`/api/script/tag`、`/api/script/version` | 配额 / 标签 / 版本     |

`status`（1=已上线 / 0=草稿）与 `exposeAs`（NONE/HTTP/MCP/BOTH）是正交的两列：
`publish` 同时置 `status=1` 并按协议生成 `http_path`、`mcp_tool_name`；
`unpublish` 两列一起回滚。`http_path` 只做展示 —— 真正的调度路径由 Controller 按 scriptCode 推导。

## 接口文档

| 入口                | 路径                            |
|-------------------|-------------------------------|
| Knife4j           | `/script/doc.html`            |
| Swagger UI        | `/script/swagger-ui/index.html` |
| OpenAPI JSON      | `/script/v3/api-docs`         |

三者实测 200，`servers.url` 会带上 context-path（`http://localhost:8086/script`）。
注意：这些是 **传递依赖** 带来的 —— 本仓任何 pom 都没有声明 knife4j / springdoc，
它们由 `z-boot-web-starter` 引入（jar 内可见 `knife4j-openapi3-spring-boot-starter-4.1.0`、
`springdoc-openapi-* 1.6.15`）。升级上游 starter 时这里会跟着动。

## 数据库

`_doc/002_deploy/init.sql`，17 张表 + 3 条幂等 seed（`hello_world` 演示脚本、`default` Mock 环境、
一个 Mock 端点），全部 `WHERE NOT EXISTS` 保护，可重复执行、不覆盖用户改动。
存量库升级（16 表时代建的库）也幂等：自动补 `z_script_api_key.app_id` 列、
由存量 Key 回填 `z_script_app` 行，重复执行不产生副作用。

| 分组 | 表                                               |
|----|--------------------------------------------------|
| 应用与鉴权 | `z_script_app`（权限中心：scope/脚本列表挂应用）、`z_script_api_key`（app_id 挂应用）、`z_script_quota` |
| 脚本 | `z_script`、`z_script_version`、`z_script_tag`、`z_script_tag_rel` |
| 日志 | `z_script_invoke_log`（调用）、`z_script_execution_log`（执行） |
| Mock | `z_mock_environment`、`z_mock_endpoint`、`z_mock_scenario`、`z_mock_scenario_state`、`z_mock_test_case`、`z_mock_recording`、`z_mock_recording_request`、`z_mock_request_log` |

`z_script_invoke_log` 列（写 SQL 时别按实体字段名猜）：
`api_key_id, app_name, script_id, script_code, invoke_ip, invoke_method, invoke_path,
invoke_params, invoke_status, http_status, duration_ms, error_message, invoked_at`。
实体 `InvokeLogDO` 用别名 getter/setter（`setPath`→`invoke_path`、`setCostMs`→`duration_ms` 等）对上这些列，
`dslType` / `requestSize` 是 `@TableField(exist=false)`，不落库。

**不预置任何 API Key。** 凭证一律不入 Git（`lead/008_组织规范/001_凭证管理规范.md`）。

## 控制台（自带 web）

- 两层前端在 `_frontend/`：`z-script-frontend`（SPA）+ `z-script-frontend-component`（纯 props 组件库），
  细节见 [_frontend/README.md](../../_frontend/README.md)。
- 构建期 `dist/` 拷进 `target/classes/static/`，运行期与 API 同源。
- 首次进入是空列表 + `MISSING_API_KEY` —— 点「签发 AK」或粘贴已有 AK，属正常。

## 部署形态

| 形态 | 入口 | 说明 |
|---|---|---|
| Mode 1 合体 | `deploy/bin/start-mode1.sh` / `make dev` | 单容器，jar 内嵌前端 |
| Mode 2 分体 | `make split` | nginx 静态 + 后端，`location /script/api/` 反代，不做路径重写 |
| Mode 3 集群 | `make cluster` | nginx 多副本 + 后端多副本 |
| K8s | `deploy/bin/k8s-apply.sh` | 必填 `INGRESS_DOMAIN DB_HOST DB_PASSWORD`；DB Secret 由脚本 `kubectl create secret` 现场建，仓库只有 ConfigMap |

探针：`/script/actuator/health/liveness`（initialDelay 60s）、
`/script/actuator/health/readiness`（30s），均 8086。`show-details: never`，
所以探针响应只有 `{"status":"UP"}`。

## 故障排查

| 现象                                        | 排查                                                           |
|-------------------------------------------|--------------------------------------------------------------|
| 启动就 NPE / 表不存在                             | 没跑 `init.sql`，或库名不对；确认 `Z_BASE_DB_SCRIPT_DATABASE`            |
| 明明配了 `spring.datasource.*` 却连到 localhost    | 键名错，只认 `z.base.db.script.*`（见上）                              |
| 启动报 `expected single matching bean but found 2` | classpath 上混进第二个自带 SqlSessionFactory 的模块；`application.yml` 已 exclude MP 自动配置，检查新加的依赖 |
| `/script/` 404                            | 用 `-Dfrontend.skip=true` 打的包，或改了 `context-path` 没同步 vite `base` |
| 所有接口 401 `MISSING_API_KEY`                 | 没带 `X-Api-Key`；控制台需要先签发/粘贴 AK                              |
| `SCOPE_NOT_ALLOWED` 但脚本看着没限制                | Key 归属应用是 `SPECIFIC`（脚本列表在控制台「应用」页签配），且调的是管理面（解析不出 scriptCode）；控制台用的应用保持 `ALL` |
| 403 `APP_DISABLED`                        | Key 归属应用被禁用；控制台「应用」页签或 `POST /api/script/app/{appCode}/enable` 恢复 |
| 带 body 的签名请求全 401 `INVALID_SIGNATURE`       | 已知限制：服务端不读 body，见「鉴权」小节                                       |
| 429 `QUOTA_EXCEEDED`                      | 默认 60/分、10000/天（`z_script_quota`），看 `Retry-After`            |
| 调用日志为空                                    | 异步线程写失败会吞异常；先确认 `z_script_invoke_log` 可写，再看 root 日志级别         |
| 发布后 `/script/run/{code}` 404               | 脚本被 `unpublish`（`exposeAs=NONE`），或 `dslType` 不在 EL/GROOVY/LUA/SQL/MOCK/API_BRIDGE 内 |
