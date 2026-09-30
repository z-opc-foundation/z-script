# z-script

> L3 中间件 · 多 DSL 脚本平台 + Mock 平台 · Spring Boot Starter 一行内嵌 · 自带 Web 控制台

一件事：**把「脚本」和「Mock」当成平台能力对外提供**。脚本侧负责沙箱执行（EL / Groovy / Lua / SQL）、
版本与灰度、标签、配额、调用审计；Mock 侧负责端点、环境、场景状态机、用例、录制回放、curl / OpenAPI 导入与故障注入。
脚本上线后可直接暴露为 HTTP 端点，也能注册成 MCP Tool。对外一律 `app + AK`（`X-Api-Key`），不挂 4A / SSO，
控制台与被调用方走同一套凭证、同一个端口。

---

## 📋 基本信息

| 字段 | 值 |
|------|-----|
| **仓库** | `z-script`（聚合 pom，`<packaging>pom</packaging>`） |
| **Maven 坐标** | `io.github.yuku123:z-script:${revision}`；库件 `z-script-core` / `-engine` / `-web` / `-scene` / `-scene-http` |
| **当前版本** | `1.0.1`（根 POM `<revision>`，CI-friendly versions + flatten-maven-plugin `flattenMode=oss`） |
| **父项目** | `io.github.yuku123:z-boot-parent:1.0.21`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘）；实测父链：地板 `z-boot-dependencies:1.0.20` + 兄弟仓权威 `z-boot-fleet:1.0.1`（fleet 里 `z-script.version=1.0.1`、`z-util.version=1.0.14`） |
| **Maven Central** | **已发布**：上述 6 个库坐标的 `1.0.0` 与 `1.0.1` 从 repo1 回读全部 200（`io.github.yuku123/<artifactId>/<version>/…-<version>.pom`）。唯一例外是应用包：`z-script-admin:1.0.0` = 200（历史漏发，`central-publishing` 插件不认 `maven.deploy.skip`），`1.0.1` = 404 —— 已在 `central` profile 用 `excludeArtifacts` 挡住 |
| **默认端口** | `8086`（`SERVER_PORT` 可覆盖）· `server.servlet.context-path: /script` |
| **运行口径** | Java 8（父链 `maven.compiler.source/target=8`，class major 52）· Spring Boot 2.7.18 |
| **最近更新** | 2026-09-30 |

> 孵化初期这批库确实还没上 Central；现在的实测结论是「已发布」，且 admin 的漏发已收口。
> 编译建议 JDK 17、运行 JDK 8（见 [`_doc/002_deploy/z-script-admin.md`](_doc/002_deploy/z-script-admin.md)）；
> CI [`publish-central.yml`](.github/workflows/publish-central.yml) 走 temurin 8：先校验标签版本形状、探一次
> repo1 占位，再 `mvn -B test -Drevision=<tag>` 跑测试（本仓没有 ci.yml，这份 publish 是唯一 CI —— 实测 36 项：
> engine 15 + scene-http 21），最后 `mvn -B -ntp deploy -Pcentral -DskipTests -Drevision=<tag>`。
> 签名口令不进命令行 argv（走 setup-java 写的 `gpg.passphraseEnvName`）。

---

## 🎯 能力清单

每一条都对应到仓里真实的类或端点（`z-script-web` 下实测 19 个 Controller，`z-script-scene-http` 再加 1 个，共 20 个）：

| 能力 | 入口 | 说明 |
|------|------|------|
| 脚本 CRUD / 执行 / 上下线 | `ScriptController` `/api/script` | `list` `byCode` `run` `publish` `unpublish` `import-curl` `import-openapi` `import` `export` `copy` |
| 版本与灰度 | `ScriptVersionController` `/api/script/version` | `publish` `list/{scriptId}` `canary` `promote` `offline` |
| 标签 | `ScriptTagController` `/api/script/tag` | `create` `bind` `unbind` `of-script/{scriptId}` |
| 已上线脚本对外调用 | `ScriptHttpDispatchController` `/api/script-run/{scriptCode}/**`（推荐）、`ScriptRunController` `/run/{scriptCode}`（历史入口） | 按 `exposeAs ∈ {HTTP, BOTH}` 放行，路由键是 `scriptCode`，`http_path` 只作展示 |
| 多 DSL 沙箱执行 | `ScriptEngine` + `engine/sandbox/` | 只认 `EL \| GROOVY \| LUA \| SQL \| MOCK \| API_BRIDGE` 六种，其它抛 `Unsupported DSL type`；`ElSandbox`（SpEL + `SimpleEvaluationContext`）、`GroovySandbox`（`SandboxPolicy` 白名单 + 超时） |
| 脚本 → MCP Tool | `ScriptMcpAdapter` | `publish` 时发事件注册；开关 `z.script.mcp-bridge.enabled`（默认 true）；宿主没有 `McpRegistry` 时打日志跳过、不监听端口 |
| Mock 管理面 | `MockPlatformController` `/api/mock-platform` | 端点 / 环境 / 场景 / 用例 CRUD + `/stats` + `scenarios/reset`、`scenarios/instances` |
| Mock 分发 | `MockDispatchController` `/api/mock/**`、`/api/mock/{envCode}/**` | 按 path 命中，`X-Mock-Env` 选环境；`MockEndpointController` 提供端点增删改查 |
| 场景状态机 / 断言 / 故障注入 | `ScenarioStateMachine`、`AssertionEngine`、`FaultInjectionEngine`、`RequestMatcherEngine`、`MockTemplateEngine` | `@pick` / `@datetime` 之类模板渲染 + 命中匹配 + 注入 |
| 录制与回放 | `MockRecordingController` `/api/mock-platform/recordings` | `start` `stop` `list` `byCode` `requests` `export` `import` `playback` `compare` `mocks` |
| 请求日志 / 用例批跑 | `MockRequestLogController`、`MockTestCaseRunnerController`（`/run`、`/run-batch`） | `RecorderEngine` / `PlaybackEngine` 落引擎层 |
| curl / OpenAPI 导入 | `MockCurlController`（`parse` `run` `import-as-case` `import-as-endpoint` `export`）、`MockOpenApiController`（`parse` `import-batch` `sample`） | 转成 `API_BRIDGE` 定义后由 `DynamicApiExecutor` + `ApiBridgeDefinition` 执行 |
| 应用与 API Key | `AppController` `/api/script/app`、`ApiKeyController` `/api/script/api-key` | 权限模型以**应用**为中心，见下一节 |
| 配额 / 重试熔断 | `QuotaController`、`RetryPolicyController` | 默认 60/分、10000/天（`QuotaServiceImpl` 初始化值），超限 429 带 `Retry-After` |
| 调用审计 | `InvokeLogController` + `InvokeLogCleanController` + `InvokeLogCleanupJob` | 异步落 `z_script_invoke_log`；清理保留 `zscript.invoke-log.retention-days`（默认 90 天），cron 默认 `0 0 3 * * ?` |
| HTTP 多步链路编排 | `HttpChainController` `/api/scene-http/chain/execute`、`/chain/preview` | `HttpChainExecutor` + `VariableReplacer`，支持 mock 响应注入与 `stopOnFail` |

---

## 🔐 鉴权模型：app + AK，且以「应用」为中心

`ZScriptWebMvcConfig` 是**默认拒绝**：`addPathPatterns("/api/**", "/run/**")`，唯一 exclude 是
`/api/script/api-key`（POST，签发第一把 Key 的引导端点 —— 此刻调用方手上还没有 Key）。
`/run/**` 必须点名，因为 `ScriptRunController` 不在 `/api` 前缀下，漏点名等于开一条免鉴权的脚本执行门。

权限挂在**应用**上，Key 只是应用的凭证：

- `z_script_api_key.app_id → z_script_app`；一个应用可签发多把 Key，随时 `/{id}/reset-secret` 重置；
  签 Key 时应用不存在会引导创建（`AppService.findOrCreate`），scope 取请求里的值。
- 应用带 `scope`（`ALL` / `SPECIFIC` / `READ_ONLY`）与 `allowed_scripts`（scriptCode 的 JSON 数组），
  经 `POST /api/script/app/scripts?appCode=` 配置；`Key` 自身的 `scope` / `allowed_scripts` 两列只作存量库回退。
- `ApiKeyAuthInterceptor` 校验链（任一步失败立即返回并写 errorCode）：
  Key 存在 → 启用 → 未过期 → IP 白名单（支持 CIDR）→ **应用状态 + 应用 scope** → 配额 → 可选 HMAC 签名 → 上下文注入 → 异步审计。
  错误码：`MISSING_API_KEY` / `INVALID_API_KEY` / `API_KEY_DISABLED` / `API_KEY_EXPIRED`（401）、
  `IP_NOT_ALLOWED` / `APP_DISABLED` / `SCOPE_NOT_ALLOWED`（403）、`QUOTA_EXCEEDED`（429）、`INVALID_SIGNATURE`（401）。
- apiKey 前缀实测 `zsk_live_`；`secret` 明文只在创建/重置时返回一次，库里存哈希。
- **禁用应用 = 名下所有 Key 一并 403 `APP_DISABLED`**；删应用时名下还有 Key 会被拒绝。
- 已知边界（如实记录）：scriptCode 从 URI 解析，管理面端点解析不出 → `SPECIFIC` 的应用调不到 `list`/`publish`，
  控制台用的应用应保持 `ALL`；`READ_ONLY` 目前是空实现（与 `ALL` 等价）；
  HMAC 签名只对 GET / 无体请求可靠（`ApiKeyAuthInterceptor.readBody()` 直读 input stream，
  javadoc 引用的 `ContentCachingRequestWrapperFilter` 实测**类不存在**，见 [`_doc/006_release/audit-incubation-1.0.0.md`](_doc/006_release/audit-incubation-1.0.0.md) WARN-004）。
- 整体关掉鉴权只留了一个开关，且是 **JVM system property**：`-Dz.script.api-key.enabled=false`（仅本机排障，生产禁用）。

---

## 🏗️ 项目结构

```
z-script/
├── pom.xml                     # 聚合 POM：parent z-boot-parent:1.0.21，<revision>=1.0.1 统一版本
├── z-script-core/              # 领域层：17 张表的实体 + MyBatis-Plus Mapper + Service（App/ApiKey/Script/Version/Tag/Quota/InvokeLog/Mock*）
├── z-script-engine/            # 引擎层：ScriptEngine + sandbox/{El,Groovy}Sandbox、MockEngine、Assertion、Recorder/Playback、FaultInjection、ScriptMcpAdapter
├── z-script-web/               # Starter 层：19 个 Controller + ApiKeyAuthInterceptor + 自动装配（spring.factories）
├── z-script-scene/             # 聚合 pom
│   └── z-script-scene-http/    # HTTP 链路编排：HttpChainExecutor + /api/scene-http
├── z-script-admin/             # 可执行单体（内嵌 SPA，exec classifier）；留 reactor 但 maven.deploy.skip=true
├── _frontend/                  # 两层 npm 工程：SPA + 纯 props 组件库（不进 Maven reactor）
├── deploy/                     # 三种部署模式 + k8s + Makefile
└── _doc/                       # 文档，见文末「文档目录」
```

依赖方向只允许向下：`admin → web → engine → core`，`scene-http → engine`。
`z-script-admin` 享受统一构建但不发布（架构规范原则 15：可运行应用不上中央仓库），
产物只作 Docker 镜像来源或本地 `java -jar` 演示。

---

## 🔧 技术栈（全部取自实测 POM / 产物 jar）

| 层级 | 技术 |
|------|------|
| 语言 / 运行时 | Java 8（父链 `java.version=1.8`，产物 class major 52） |
| 框架 | Spring Boot 2.7.18（由父链 `z-boot-parent:1.0.21` → 地板 `z-boot-dependencies:1.0.20` 供给） |
| 兄弟仓权威 | `z-boot-fleet:1.0.1`（本仓 `z-script-web` 直接引 `z-boot-web-starter` / `z-boot-datasource-starter`） |
| 工具 | `z-util-core` / `z-util-http` / `z-util-parser-json` / `z-util-expr-{el,groovy,lua,sql,js}` — 实测解析到 `1.0.14` |
| 持久层 | MyBatis-Plus 3.5.7 + mybatis 3.5.16 + Druid 1.2.23（均由地板传递，本仓不钉版本） |
| 数据库 | MySQL 8（`mysql-connector-j` 8.4.0） |
| 脚本引擎 | Groovy 3.0.9、luaj-jse 3.0.1、spring-expression（SpEL）；`API_BRIDGE` 走 `DynamicApiExecutor` |
| 接口文档 | 直接依赖只有 `swagger-annotations` 2.2.8；Knife4j 4.1.0 / springdoc-openapi 1.6.15 是 `z-boot-web-starter` 的传递依赖，升级上游会跟着动 |
| 测试 | JUnit 5.11.4 + platform 1.11.4 + surefire 3.5.4 —— 本仓刻意钉死（父链供 5.9.3 / 2.22.2，混旧 platform 会让 surefire 在 forked JVM 抛 `NoSuchMethodError`） |
| 前端 | React 18 + antd 5 + Vite 5（`_frontend/`，独立 npm 工程）；构建期 `frontend-maven-plugin` 1.15.0 自装 node `v18.17.0` / npm `9.6.7` |
| 构建 / 发布 | Maven（flatten `oss` 模式 + `central` profile：source/javadoc/GPG/central-publishing 0.7.0）· Docker / k8s |

---

## 🚀 快速开始

### 编译 + 打包（含控制台前端）

```bash
# 有 node 环境时由 frontend-maven-plugin 自己装 node，无需本机预装
mvn -pl z-script-admin -am package              # 产物 z-script-admin/target/z-script-admin-1.0.1-exec.jar
mvn -pl z-script-admin -am package -Dfrontend.skip=true   # 只发库/CI：跳过前端，jar 能出但 /script/ 404
```

第三方版本一律由 `z-boot-parent` → `z-boot-dependencies`（地板）+ `z-boot-fleet`（权威表）供给，
模块 POM 里不应再出现字面版本钉；报找不到版本时先确认能解析到 `io.github.yuku123:z-boot-parent:1.0.21`。

### 建库

```bash
mysql -h127.0.0.1 -P3306 -uroot -e "CREATE DATABASE IF NOT EXISTS z_script CHARACTER SET utf8mb4"
mysql -h127.0.0.1 -P3306 -uroot z_script < _doc/002_deploy/init.sql   # 幂等：17 张表 + 3 条演示 seed（hello_world 脚本 / default Mock 环境 / 一个 Mock 端点）+ 由存量 Key 回填 z_script_app；不预置任何 Key
```

### 起服务

```bash
java -jar z-script-admin/target/z-script-admin-1.0.1-exec.jar \
    --z.base.db.script.host=127.0.0.1 --z.base.db.script.username=root
# 控制台 http://localhost:8086/script/ · Knife4j /script/doc.html · /script/swagger-ui/index.html · /script/v3/api-docs
```

没有 MySQL 时先用 compose 起个试用库：`cd deploy && docker compose --profile with-db up -d`（库落在 `127.0.0.1:13306`，自动跑 init.sql），
再用 `Z_BASE_DB_SCRIPT_PORT=13306` 指过去。profile 只有 `dev`：把 `com.zifang.z.script` 日志开到 DEBUG，**鉴权不放宽**。

### 数据源配置（键名容易踩坑）

走 z-boot-datasource-starter 的 **module 键**，即 `z.base.db.script.*`，由 `ScriptMyBatisConfig#buildDataSource` 装配
（`dataSourceScript` + `sqlSessionFactoryScript`，并 exclude 掉 `MybatisPlusAutoConfiguration`）。
**写成 `spring.datasource.script.*` 不报错，会静默回落到 `localhost/root/空密码/z_script`**，是孵化期最难查的一类故障。

| 环境变量 | 对应配置键 | 默认 |
|----------|-----------|------|
| `Z_BASE_DB_SCRIPT_HOST` | `z.base.db.script.host` | `localhost` |
| `Z_BASE_DB_SCRIPT_PORT` | `z.base.db.script.port` | `3306` |
| `Z_BASE_DB_SCRIPT_DATABASE` | `z.base.db.script.database` | `z_script` |
| `Z_BASE_DB_SCRIPT_USERNAME` | `z.base.db.script.username` | `root` |
| `Z_BASE_DB_SCRIPT_PASSWORD` | `z.base.db.script.password` | 空 —— **生产必须经环境变量 / K8s Secret 注入，禁止写进 yml、jar、镜像层** |

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

### 作为 Starter 内嵌进业务应用

```xml
<dependency>
    <groupId>io.github.yuku123</groupId>
    <artifactId>z-script-web</artifactId>
    <version>1.0.1</version>
</dependency>
```

版本交给 `z-boot-fleet` 锁也可以（fleet 1.0.1 的 `z-script.version` 正是 `1.0.1`）。引入即自动装配：
`META-INF/spring.factories` → `ZScriptWebAutoConfiguration`（`@ComponentScan("com.zifang.z.script")` + 6 个引擎 Bean）；
要用链路编排再单独引 `z-script-scene-http`（其 `SceneHttpAutoConfiguration` 的 `spring.factories` 键已在孵化时补齐）。
z-boot 侧另有聚合 starter `z-boot-script-starter`（依赖 `z-script-web`），进程内跑整套脚本/Mock 能力，无需独立部署 admin。

### 调一个脚本

```bash
# 1. 引导：唯一免鉴权端点，应用不存在会自动创建；secret 只在这一次返回
curl -X POST http://localhost:8086/script/api/script/api-key \
     -H 'Content-Type: application/json' \
     -d '{"appName":"z-qa","scope":"ALL","description":"QA 平台调用"}'

# 2. 之后一律带 AK
curl -H "X-Api-Key: zsk_live_xxx" http://localhost:8086/script/api/script/list

# 3. 治理：给应用配脚本列表（scope=SPECIFIC + scriptCode 数组）
curl -X POST 'http://localhost:8086/script/api/script/app/scripts?appCode=z-qa' \
     -H "X-Api-Key: zsk_live_xxx" -H 'Content-Type: application/json' \
     -d '{"scope":"SPECIFIC","scripts":["hello_world"]}'

# 4. 执行：Ad-hoc 与已上线的 HTTP 暴露路径
curl -X POST 'http://localhost:8086/script/api/script/run?scriptCode=hello_world' \
     -H "X-Api-Key: zsk_live_xxx" -H 'Content-Type: application/json' -d '{"name":"zifang"}'
curl 'http://localhost:8086/script/api/script-run/hello_world?name=zifang' -H "X-Api-Key: zsk_live_xxx"
curl http://localhost:8086/script/api/mock/hello -H "X-Api-Key: zsk_live_xxx"   # Mock 按 path 命中
```

`EL` 是 SpEL，跑在 `SimpleEvaluationContext` 沙箱里，入参以 `#name` 引用；类型 / 构造器 / bean 引用被拒即抛 `ScriptSecurityException`。

---

## 🔌 API 一览

统一前缀 `/script`（context-path）+ `/api/**` 受 AK 拦截。除 `/api/script/api-key`（POST 引导）外全部要带 `X-Api-Key`。

| 路径 | Controller | 用途 |
|------|------------|------|
| `/api/script` | `ScriptController` | 脚本 CRUD / `run` / `publish` / `unpublish` / `import-curl` / `import-openapi` / `copy` / `export` / `preview-mapping` |
| `/api/script-run/{scriptCode}/**` | `ScriptHttpDispatchController` | 已发布为 HTTP 的脚本（推荐入口，任意方法） |
| `/run/{scriptCode}` | `ScriptRunController` | 同一能力的历史入口（GET/POST，`Result` 包装） |
| `/api/script/version` | `ScriptVersionController` | 版本发布 / 灰度 / 转正 / 下线 |
| `/api/script/tag` | `ScriptTagController` | 标签与绑定 |
| `/api/script/app` | `AppController` | 应用列表 / 创建 / 更新 / `scripts`（配可访问脚本）/ `enable`\|`disable` / 删除 |
| `/api/script/api-key` | `ApiKeyController` | 签发（唯一免鉴权）/ `page` / `list-enabled` / `reset-secret` / `enable` / `disable` |
| `/api/script/quota` | `QuotaController` | 按 Key 查改配额、`reset-daily` |
| `/api/script/retry` | `RetryPolicyController` | 重试策略与熔断状态 / `reset` |
| `/api/script/invoke-log`、`/invoke-log/clean` | `InvokeLogController`、`InvokeLogCleanController` | 分页 + `stats/*` 汇总、手动清理 + 配置查询 |
| `/api/mock`、`/api/mock/{envCode}/**` | `MockEndpointController`、`MockDispatchController` | Mock 端点管理 + 按 path 命中分发（`X-Mock-Env`） |
| `/api/mock-platform` | `MockPlatformController` | 端点 / 环境 / 场景 / 用例管理 + `/stats` |
| `/api/mock-platform/curl`、`/openapi` | `MockCurlController`、`MockOpenApiController` | curl / OpenAPI 解析、导入、导出 |
| `/api/mock-platform/recordings`、`/request-logs`、`/cases` | `MockRecordingController`、`MockRequestLogController`、`MockTestCaseRunnerController` | 录制启停、回放对比、请求日志、用例单跑/批跑 |
| `/api/scene-http/chain/{execute,preview}` | `HttpChainController` | HTTP 多步链路编排 |
| `/actuator/health`、`/actuator/info` | Spring Boot Actuator | 只暴露这两个，`show-details: never`；`probes.enabled=true` 后才有 `health/{liveness,readiness}` |

---

## 🧪 测试

```bash
mvn test
```

实测仓内只有 3 个测试类，全是纯单元测试，**不连数据库、不起网络**：
`z-script-engine` 的 `ScriptSandboxTest`（沙箱放行普通算术，拦下 `System.exit`、`Runtime.exec`、
全限定 Runtime、`String.execute` RCE、`Class.forName` 反射、`getClass` 逃逸），
`z-script-scene-http` 的 `HttpChainExecutorTest`（空链路 / 变量替换 / 断言失败 / `stopOnFail` /
mock 端点响应注入 / JSON round-trip）与 `VariableReplacerTest`。

已知口径：本仓刻意把 JUnit 钉在 5.11.4 / platform 1.11.4 并要求 surefire 3.5.4（写成 import BOM 顶不过
父链继承来的直接 DM），跟着父链降回 2.22.2 会同时换掉执行器与用例计数口径。`z-script-core` / `z-script-web`
目前没有测试类 —— 鉴权链与 Controller 的行为靠 [`_doc/002_deploy/z-script-admin.md`](_doc/002_deploy/z-script-admin.md)
里的 curl 清单人工回归，改拦截器请照着 `_doc/006_release/audit-incubation-1.0.0.md` 的复核命令走一遍。

---

## 🐳 部署

```bash
cd deploy
make help                                     # dev / split / cluster / build / push / k8s-apply / ps / logs / down / clean
make dev WITH_DB=1                            # Mode 1 合体：单容器，jar 内嵌前端
make split                                    # Mode 2 分体：nginx 静态 + 后端 jar
make cluster N=3                              # Mode 3 集群：1 前端 + N 后端
DB_HOST=... INGRESS_DOMAIN=example.com bash bin/k8s-apply.sh   # 另需 DB_PASSWORD，Secret 由脚本现场创建
```

镜像：[`deploy/Dockerfile.backend`](deploy/Dockerfile.backend)（来自 `z-script-admin` exec jar，JRE 8）+
[`deploy/Dockerfile.frontend`](deploy/Dockerfile.frontend)（直拷 `dist`，容器内不再跑 vite build，nginx 模板见
[`deploy/nginx.conf.template`](deploy/nginx.conf.template)）；compose 三件套
[`docker-compose.yml`](deploy/docker-compose.yml) / [`docker-compose.split.yml`](deploy/docker-compose.split.yml) /
[`docker-compose.cluster.yml`](deploy/docker-compose.cluster.yml)；k8s 清单
[`deploy/k8s/`](deploy/k8s/) 按 `00-namespace` → `06-configmap` 顺序 apply；
变量名清单见 [`deploy/env/.env.example`](deploy/env/.env.example)（`IMAGE_VERSION` / `JAVA_OPTS` / `OCI_REGISTRY` /
`Z_BASE_DB_SCRIPT_*`）。探针：`/script/actuator/health/liveness`（initialDelay 60s）与 `readiness`（30s）。
三种模式的取舍与验证清单在 [`deploy/README.md`](deploy/README.md)。

### 发布到 Maven Central

```bash
mvn -B deploy -Pcentral            # 在仓库根执行：flatten(oss) + source/javadoc + GPG + central-publishing
```

`central` profile 只发 `z-script` / `-core` / `-engine` / `-web` / `-scene` / `-scene-http`，
并用 `excludeArtifacts` 显式挡住 `z-script-admin`（该插件**不认** `maven.deploy.skip`，1.0.0 就是因此漏上去的）。
CI 走 tag：推 `v1.0.x` 触发 [`publish-central.yml`](.github/workflows/publish-central.yml)，由 tag 反解 `revision`
（先校验 `x.y.z[-后缀]` 形状，再探 repo1 —— 已占位的版本当场拒，别等 409 才知道撤不回来）。

⚠️ 实测 [`_doc/003_script/build.sh`](_doc/003_script/build.sh) 与 [`package.sh`](_doc/003_script/package.sh) 是模板残留：
二者 `SCRIPT_DIR` 落在 [`_doc/003_script/`](_doc/003_script/)，却要求该目录有 `pom.xml` 与 `Dockerfile`（都没有），
`build.sh` 还 `-pl z-script-server` —— 本仓没有这个模块，**原位直接跑必挂**。
[`deploy_maven_center.sh`](_doc/003_script/deploy_maven_center.sh) 同理（`cd "$(dirname "$0")"` 后校验 pom.xml），
要用就得先拷到仓库根；正常发布以上面根目录的 `mvn -B deploy -Pcentral` 或 CI 为准。

---

## 🔒 凭证与安全

**本仓库不含任何明文凭证**：本地 profile 文件（`application-{local,dev,prod}.yml`）已在 `.gitignore` 内，
仓内只有 [`z-script-admin/src/main/resources/application.yml`](z-script-admin/src/main/resources/application.yml)，
其中所有连接信息都是 `${ENV:默认}` 占位；`init.sql` **刻意不预置任何 API Key**（Key 由引导端点或控制台现场签发，
`secret` 明文只返回一次、库内只存哈希）；K8s 的 DB Secret 由 `bin/k8s-apply.sh` 现场创建，仓里只留 ConfigMap。
参考规范：[z-opc-foundation-lead/008_组织规范/001_凭证管理规范.md](https://github.com/z-opc-foundation/z-opc-foundation-lead/blob/main/008_%E7%BB%84%E7%BB%87%E8%A7%84%E8%8C%83/001_%E5%87%AD%E8%AF%81%E7%AE%A1%E7%90%86%E8%A7%84%E8%8C%83.md)

---

## 📄 License

MIT License — 详见根 [`LICENSE`](LICENSE)（版权方 z-opc-foundation）；根 POM `<licenses>` 同样声明 MIT。

_Maintained by the z-opc-foundation organization._

---

## 文档目录

本项目文档统一收口在 `_doc/` 下：

- [`_doc/001_arch/`](_doc/001_arch/) — 架构文档：
  - [`00-overview.md`](_doc/001_arch/00-overview.md) — 模块边界、鉴权链与 scope 语义、17 表数据模型、
    引擎与沙箱、HTTP 面、孵化期间 17 条整改及理由、已知不一致（§9）。
    *注：文中模块图写的 `${revision} = 1.0.0` 已过期，当前是 1.0.1。*

- [`_doc/002_deploy/`](_doc/002_deploy/) — 部署与运行：
  - [`init.sql`](_doc/002_deploy/init.sql) — 建库建表 + 幂等 seed（17 张表 + 3 条），可反复执行，含 `information_schema` 自检
  - [`z-script-admin.md`](_doc/002_deploy/z-script-admin.md) — 运行手册：端口与路径、启动与打包、配置项、
    鉴权错误码表、API 速查、接口文档入口、表结构、控制台、部署形态、故障排查

- [`_doc/003_script/`](_doc/003_script/) — 构建与发布脚本：
  - [`deploy_maven_center.sh`](_doc/003_script/deploy_maven_center.sh) — Central 发布封装（`publish` / `verify` / `gpg-init` / `readme`；须在仓库根运行，见上文 ⚠️）
  - [`install-settings.sh`](_doc/003_script/install-settings.sh) — 往 `~/.m2/settings.xml` 注入 `<server id="central">`，用环境变量占位不落明文
  - [`build.sh`](_doc/003_script/build.sh) · [`package.sh`](_doc/003_script/package.sh) — **模板残留，本仓不可用**（引用不存在的 `pom.xml` / `Dockerfile` / `z-script-server` 模块）

- `_doc/004_skill/` — AI skill 与审计记录：
  - [`audit-incubation-1.0.0.md`](_doc/006_release/audit-incubation-1.0.0.md) — 1.0.0 孵化入库审计：WARN-001（`/run/**` 免鉴权）、WARN-002（SPECIFIC 恒 403）、WARN-003（K8s 探针路径）已修；WARN-004（带 body 的 HMAC 验签）已记录未修；WARN-005 为文档型；附复核命令

配套文档（不在 `_doc/` 下，链接同样实测有效）：
[`_frontend/README.md`](_frontend/README.md) — 两层 npm 工程与 vite `base` 必须等于 `/script/` 的约定；
[`deploy/README.md`](deploy/README.md) — 三种部署模式 + K8s 细节。

## 关联

- 父组织：[z-opc-foundation](https://github.com/z-opc-foundation)
- L2 封装：[z-boot](https://github.com/z-opc-foundation/z-boot)（`z-boot-parent` / `z-boot-web-starter` / `z-boot-datasource-starter` / `z-boot-script-starter`）
- L1 工具：[z-util](https://github.com/z-opc-foundation/z-util)（`z-util-core/http/parser-json/expr-*`）
