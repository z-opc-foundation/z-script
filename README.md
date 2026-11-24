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
| **当前版本** | `1.0.3`（根 POM `<revision>`，CI-friendly versions + flatten-maven-plugin `flattenMode=oss`） |
| **父项目** | `io.github.yuku123:z-boot-parent:1.2.4`（`<relativePath/>` 留空，parent 在 repo1 不在磁盘）；父链自带第三方地板与兄弟仓权威表（其中 `z-script.version=1.0.3`、`z-util.version=1.0.14`） |
| **Maven Central** | **已发布**：上述 6 个库坐标的 `1.0.0` 与 `1.0.1` 从 repo1 回读全部 200（`io.github.yuku123/<artifactId>/<version>/…-<version>.pom`）。唯一例外是应用包：`z-script-admin:1.0.0` = 200（历史漏发，`central-publishing` 插件不认 `maven.deploy.skip`），`1.0.1` = 404 —— 已在 `central` profile 用 `excludeArtifacts` 挡住 |
| **默认端口** | `8086`（`SERVER_PORT` 可覆盖）· `server.servlet.context-path: /script` |
| **运行口径** | Java 17（父链 `maven.compiler.source/target=17`，class major 61）· Spring Boot 3.3.13 |
| **最近更新** | 2026-09-30 |

> 孵化初期这批库确实还没上 Central；现在的实测结论是「已发布」，且 admin 的漏发已收口。
> 编译建议 JDK 17、运行 JDK 8（见 [`_doc/002_deploy/01-deploy-runbook.md`](_doc/002_deploy/01-deploy-runbook.md)）；
> CI [`publish-central.yml`](.github/workflows/publish-central.yml) 走 temurin 8：先校验标签版本形状、探一次
> repo1 占位，再 `mvn -B test -Drevision=<tag>` 跑测试（本仓没有 ci.yml，这份 publish 是唯一 CI —— 实测 36 项：
> engine 15 + scene-http 21），最后 `mvn -B -ntp deploy -Pcentral -DskipTests -Drevision=<tag>`。
> 签名口令不进命令行 argv（走 setup-java 写的 `gpg.passphraseEnvName`）。

---

## 🎯 能力清单

每一条都对应到仓里真实的类或端点（`z-script-web` 下实测 20 个 Controller，`z-script-scene-http` 再加 1 个，共 21 个）：

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
  javadoc 引用的 `ContentCachingRequestWrapperFilter` 实测**类不存在**，见 [`_doc/006_release/01-audit-incubation-1.0.0.md`](_doc/006_release/01-audit-incubation-1.0.0.md) WARN-004）。
- 整体关掉鉴权只留了一个开关，且是 **JVM system property**：`-Dz.script.api-key.enabled=false`（仅本机排障，生产禁用）。

---

## 🏗️ 项目结构

```
z-script/
├── pom.xml                     # 聚合 POM：parent z-boot-parent:1.2.4，<revision>=1.0.3 统一版本
├── z-script-core/              # 领域层：17 张表的实体 + MyBatis-Plus Mapper + Service（App/ApiKey/Script/Version/Tag/Quota/InvokeLog/Mock*）
├── z-script-engine/            # 引擎层：ScriptEngine + sandbox/{El,Groovy}Sandbox、MockEngine、Assertion、Recorder/Playback、FaultInjection、ScriptMcpAdapter
├── z-script-web/               # Starter 层：20 个 Controller + ApiKeyAuthInterceptor + 自动装配（spring.factories）
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
| 语言 / 运行时 | Java 17（父链 `java.version=17`，产物 class major 61） |
| 框架 | Spring Boot 2.7.18（由父链 `z-boot-parent:1.2.4` 自带的第三方地板供给） |
| 兄弟仓权威 | `z-boot-parent` 的兄弟仓权威表（本仓 `z-script-web` 直接引 `z-boot-web-starter` / `z-boot-datasource-starter`） |
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
mvn -pl z-script-admin -am package              # 产物 z-script-admin/target/z-script-admin-1.0.3-exec.jar
mvn -pl z-script-admin -am package -Dfrontend.skip=true   # 只发库/CI：跳过前端，jar 能出但 /script/ 404
```

第三方版本一律由 `z-boot-parent:1.2.4` 供给（第三方地板与兄弟仓权威表均已并入 parent 自身），
模块 POM 里不应再出现字面版本钉；报找不到版本时先确认能解析到 `io.github.yuku123:z-boot-parent:1.2.4`。

### 建库

```bash
mysql -h127.0.0.1 -P3306 -uroot -e "CREATE DATABASE IF NOT EXISTS z_script CHARACTER SET utf8mb4"
mysql -h127.0.0.1 -P3306 -uroot z_script < _doc/002_deploy/init.sql   # 幂等：17 张表 + 3 条演示 seed（hello_world 脚本 / default Mock 环境 / 一个 Mock 端点）+ 由存量 Key 回填 z_script_app；不预置任何 Key
```

### 起服务

```bash
java -jar z-script-admin/target/z-script-admin-1.0.3-exec.jar \
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
    <version>1.0.3</version>
</dependency>
```

版本交给 `z-boot-parent` 的兄弟仓权威表锁也可以（其 `z-script.version` 正是 `1.0.3`）。引入即自动装配：
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

仓内的测试类全是纯单元测试，**不连数据库、不起网络**：
`z-script-engine` 的 `ScriptSandboxTest`（沙箱放行普通算术，拦下 `System.exit`、`Runtime.exec`、
全限定 Runtime、`String.execute` RCE、`Class.forName` 反射、`getClass` 逃逸），
`z-script-scene-http` 的 `HttpChainExecutorTest`（空链路 / 变量替换 / 断言失败 / `stopOnFail` /
mock 端点响应注入 / JSON round-trip）与 `VariableReplacerTest`，
以及 `z-script-core` 的 `ReadmeClaimsTest`（本 README 定量主张与源码重算值的对账守卫）。

已知口径：本仓刻意把 JUnit 钉在 5.11.4 / platform 1.11.4 并要求 surefire 3.5.4（写成 import BOM 顶不过
父链继承来的直接 DM），跟着父链降回 2.22.2 会同时换掉执行器与用例计数口径。`z-script-web`
目前没有测试类 —— 鉴权链与 Controller 的行为靠 [`_doc/002_deploy/01-deploy-runbook.md`](_doc/002_deploy/01-deploy-runbook.md)
里的 curl 清单人工回归，改拦截器请照着 `_doc/006_release/01-audit-incubation-1.0.0.md` 的复核命令走一遍。

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

- [`_doc/008_troubleshooting/`](_doc/008_troubleshooting/) — 踩坑与失败要点（测试必须跑在 JDK 17 或更低）
本项目文档统一收口在 `_doc/` 下：

- [`_doc/001_arch/`](_doc/001_arch/) — 架构文档：
  - [`00-overview.md`](_doc/001_arch/00-overview.md) — 模块边界、鉴权链与 scope 语义、17 表数据模型、
    引擎与沙箱、HTTP 面、孵化期间 17 条整改及理由、已知不一致（§9）。
    *注：文中模块图写的 `${revision} = 1.0.0` 已过期，当前是 1.0.1。*

- [`_doc/002_deploy/`](_doc/002_deploy/) — 部署与运行：
  - [`init.sql`](_doc/002_deploy/init.sql) — 建库建表 + 幂等 seed（17 张表 + 3 条），可反复执行，含 `information_schema` 自检
  - [`01-deploy-runbook.md`](_doc/002_deploy/01-deploy-runbook.md) — 运行手册：端口与路径、启动与打包、配置项、
    鉴权错误码表、API 速查、接口文档入口、表结构、控制台、部署形态、故障排查

- [`_doc/003_script/`](_doc/003_script/) — 构建与发布脚本：
  - [`deploy_maven_center.sh`](_doc/003_script/deploy_maven_center.sh) — Central 发布封装（`publish` / `verify` / `gpg-init` / `readme`；须在仓库根运行，见上文 ⚠️）
  - [`install-settings.sh`](_doc/003_script/install-settings.sh) — 往 `~/.m2/settings.xml` 注入 `<server id="central">`，用环境变量占位不落明文
  - [`build.sh`](_doc/003_script/build.sh) · [`package.sh`](_doc/003_script/package.sh) — **模板残留，本仓不可用**（引用不存在的 `pom.xml` / `Dockerfile` / `z-script-server` 模块）

- `_doc/004_skill/` — AI skill 与审计记录：
  - [`01-audit-incubation-1.0.0.md`](_doc/006_release/01-audit-incubation-1.0.0.md) — 1.0.0 孵化入库审计：WARN-001（`/run/**` 免鉴权）、WARN-002（SPECIFIC 恒 403）、WARN-003（K8s 探针路径）已修；WARN-004（带 body 的 HMAC 验签）已记录未修；WARN-005 为文档型；附复核命令

配套文档（不在 `_doc/` 下，链接同样实测有效）：
[`_frontend/README.md`](_frontend/README.md) — 两层 npm 工程与 vite `base` 必须等于 `/script/` 的约定；
[`deploy/README.md`](deploy/README.md) — 三种部署模式 + K8s 细节。

## 关联

- 父组织：[z-opc-foundation](https://github.com/z-opc-foundation)
- L2 封装：[z-boot](https://github.com/z-opc-foundation/z-boot)（`z-boot-parent` / `z-boot-web-starter` / `z-boot-datasource-starter` / `z-boot-script-starter`）
- L1 工具：[z-util](https://github.com/z-opc-foundation/z-util)（`z-util-core/http/parser-json/expr-*`）

<!-- icon: minimax image-01 placeholder -->

---

<!-- z-opc-foundation-sibling-registry:v1 -->
## 🔗 兄弟仓全景（z-opc-foundation 组织）

本仓隶属于 [z-opc-foundation](https://github.com/orgs/z-opc-foundation/repositories) 组织——一人公司基座的全部自研件（现存 61 仓，含私有仓；口径 2026-11-24 GitHub org API 实读）。

| 仓 | 一句话说明 |
|---|---|
| [z-agent](https://github.com/z-opc-foundation/z-agent) | 云端 Agent 平台的库与 starter——ReAct/Plan 实现 + 注册中心 + REST 网关 + 只读控制面 |
| [z-agent-kernel](https://github.com/z-opc-foundation/z-agent-kernel) | Agent 框架基座——消息/工具/记忆/provider/会话 SPI + 轻量默认实现（纯 POJO 无 Spring） |
| [z-asset](https://github.com/z-opc-foundation/z-asset) | 企业资产台账——实物设备全生命周期 + 域名/证书/许可到期提醒 |
| [z-audit](https://github.com/z-opc-foundation/z-audit) | 审计与合规留痕——事件模型 + 哈希链 + 事件出口 SPI + 留存期策略 |
| [z-biz](https://github.com/z-opc-foundation/z-biz) | 商务外壳域——客户/合同/票据/对账四聚合 |
| [z-biz-erp](https://github.com/z-opc-foundation/z-biz-erp) | 进销存 ERP——商品/客商/采购/销售/库存流水/应收应付核销（z-biz 业务线） |
| [z-biz-security](https://github.com/z-opc-foundation/z-biz-security) | 业务数据安全防护入口——场景化策略注册 + 安全线四段流水线对外编排 |
| [z-boot](https://github.com/z-opc-foundation/z-boot) | Spring Boot Starter 聚合仓 + 版本权威（火车制）——唯一消费入口 z-boot-parent |
| [z-bot](https://github.com/z-opc-foundation/z-bot) | 本地 agent 应用——终端 CLI + HTTP/SSE web 控制台 + 多通道 gateway（飞书/钉钉/webhook） |
| [z-cache](https://github.com/z-opc-foundation/z-cache) | RESP2 协议兼容的分布式内存缓存——Java 8 + Netty，Redis 客户端零修改直连 |
| [z-camuda](https://github.com/z-opc-foundation/z-camuda) | 【已退役·归档】基于 Camunda 的工作流封装层（原 z-wf 改名，与现行 z-wf 无共同历史） |
| [z-cognition](https://github.com/z-opc-foundation/z-cognition) | 业务认知域/认知舱——术语/概念/证据链 + memory/kb/ontology 渐进式模块 |
| [z-config](https://github.com/z-opc-foundation/z-config) | 分布式配置中心 + 服务注册中心（类 Nacos）——Netty 长连接推送 + React 控制台 |
| [z-ctc](https://github.com/z-opc-foundation/z-ctc) | 4A 中心——认证/授权/账户/审计，多租户 JWT + SSO 跨子域 |
| [z-dms](https://github.com/z-opc-foundation/z-dms) | 数据源管控底座——统一登记、统一建池、统一探活、变更统一推送 |
| [z-env](https://github.com/z-opc-foundation/z-env) | 本地中间件与基础设施环境——31 个 docker-compose 场景 + make 一键运维 |
| [z-etl](https://github.com/z-opc-foundation/z-etl) | ETL 引擎——DataX 式线性作业 + Flink 式 DAG 流管道（键控状态/水位/窗口） |
| [z-ext](https://github.com/z-opc-foundation/z-ext) | Java 扩展点框架——@ExtPoint/@ExtImpl 运行期选实现，五档侵入方式 |
| [z-frontend-common](https://github.com/z-opc-foundation/z-frontend-common) | 全仓共享前端基座——AppLayout/LoginPage 等前端规范的唯一承载 |
| [z-frontend-markdown](https://github.com/z-opc-foundation/z-frontend-markdown) | Markdown 通用共享组件包（npm）——MarkdownLiveEditor/NoteMarkdown/MermaidBlock 单一真源 |
| [z-frontend-render](https://github.com/z-opc-foundation/z-frontend-render) | 平台共享前端渲染引擎——后端 schema 是唯一事实源，报表与低代码共用 |
| [z-graph](https://github.com/z-opc-foundation/z-graph) | 独立图数据库——MVCC 版本链存储 + OpenCypher 子集 + Bolt 风格协议 + React 控制台 |
| [z-gw](https://github.com/z-opc-foundation/z-gw) | 自研高性能 API 网关——Netty 手写运行时，Route/Predicate/Filter 模型可嵌 Spring Boot |
| [z-indexer](https://github.com/z-opc-foundation/z-indexer) | 代码索引服务——仓库注册表 + 定时重建索引 + 符号/调用链查询 |
| [z-introduce](https://github.com/z-opc-foundation/z-introduce) | 组织仓库展示站——全仓实测元数据汇总成可搜索静态站点 |
| [z-lc](https://github.com/z-opc-foundation/z-lc) | 低代码平台——DB 驱动通用 CRUD 引擎 + 报表/模型设计框架 |
| [z-llm-agent](https://github.com/z-opc-foundation/z-llm-agent) | CLI Agent 服务化——把本机 CLI agent（CC/Hermes/Qoder 等）暴露成 OpenAI 兼容对话 API |
| [z-llm-gateway](https://github.com/z-opc-foundation/z-llm-gateway) | LLM 网关——OpenAI/Anthropic 双协议面 + 多 vendor 凭据池、限流与 token 记账 |
| [z-llm-msghub](https://github.com/z-opc-foundation/z-llm-msghub) | msgHub 中心消息路由——WebSocket 中心枢纽 + Agent 侧客户端 SDK |
| [z-mask](https://github.com/z-opc-foundation/z-mask) | 脱敏与遮蔽引擎——按敏感命中位置遮蔽，策略即数据 |
| [z-mcp](https://github.com/z-opc-foundation/z-mcp) | MCP 服务端 + 上游聚合网关——Java 8 手写协议层，Spring Boot 自动装配 |
| [z-meta](https://github.com/z-opc-foundation/z-meta) | 元数据管理平台——租户/应用/接口清单登记 + @BizModel 业务模型采集 |
| [z-mill](https://github.com/z-opc-foundation/z-mill) | 资料磨坊——全格式文档转 Markdown/JSON/纯文本/HTML（对标 MinerU） |
| [z-mist](https://github.com/z-opc-foundation/z-mist) | KMS 分布式密钥/秘密管理平台——集中加密存储 + Netty 协议 + starter 嵌入 |
| [z-mq](https://github.com/z-opc-foundation/z-mq) | 自研分布式消息队列——NameServer+Broker+Store，形状对标 RocketMQ、自有 Netty 协议 |
| [z-msg](https://github.com/z-opc-foundation/z-msg) | 多通道消息 + WebSocket 实时下发基础设施——短信/邮件/站内信/Webhook/IM 平台 |
| [z-observer](https://github.com/z-opc-foundation/z-observer) | 可观测性域——指标/日志/链路 + 告警引擎 |
| [z-opc](https://github.com/z-opc-foundation/z-opc) | 一人公司统一后端+统一前端——单 JVM all-in-one 承载协作/Agent/RPA/交易/资产等业务域与自研中间件 |
| [z-opc-foundation-lead](https://github.com/z-opc-foundation/z-opc-foundation-lead) | 组织治理大脑——事实表、强制规范、跨仓工具集、新机器恢复钥匙（本清单权威持有处） |
| [z-opcs](https://github.com/z-opc-foundation/z-opcs) | 任务撮合/服务市场——用户付费发布诉求，持有者接单完成，金额自动瓜分 |
| [z-ops](https://github.com/z-opc-foundation/z-ops) | 个人公司运维平台——部署单元/镜像构建/WebShell/K8s 巡检 + Ops Agent 执行器 |
| [z-oss](https://github.com/z-opc-foundation/z-oss) | 对象存储服务——自持 local 后端 + aliyun/s3 对接层，starter 注入 OssTemplate |
| [z-product](https://github.com/z-opc-foundation/z-product) | 产品设计信息汇总仓（开发域 PM 工件） |
| [z-qa](https://github.com/z-opc-foundation/z-qa) | 步骤式 API 链路测试平台——用例/计划/执行/报告/质量看板（对标 TestRail·MeterSphere） |
| [z-rpa](https://github.com/z-opc-foundation/z-rpa) | RPA 产品皮——应用市场/需求中心（执行归 z-script，触发归 z-schedule） |
| [z-rpc](https://github.com/z-opc-foundation/z-rpc) | Java 8 + Netty RPC 框架——微内核 + SPI 插件化，形状参照 Dubbo/gRPC |
| [z-schedule](https://github.com/z-opc-foundation/z-schedule) | 分布式任务调度中枢（对标 XXL-Job）——选主 + 时间环排期，starter 嵌入宿主 |
| [z-script](https://github.com/z-opc-foundation/z-script) | 多 DSL 脚本平台 + Mock 平台——沙箱执行/版本灰度/可注册 MCP Tool，starter 内嵌 **（本仓）** |
| [z-security](https://github.com/z-opc-foundation/z-security) | 数据安全域编排层——场景→策略→识别→脱敏→水印→审计 四段流水线编排 |
| [z-sense](https://github.com/z-opc-foundation/z-sense) | 敏感识别与分类分级引擎——数据安全线底座（规则目录 + 可插拔识别器 + 分类分级） |
| [z-skill](https://github.com/z-opc-foundation/z-skill) | Skill 平台聚合器——归一化各家 Skill 形状，再按各家 API 形状发回 |
| [z-surl](https://github.com/z-opc-foundation/z-surl) | 短网址服务——短码生成/解析/命中统计，starter 一行集成 |
| [z-task](https://github.com/z-opc-foundation/z-task) | 任务协作平台——项目空间/看板/任务卡片（看板范式，对标 Trello·Teambition） |
| [z-team](https://github.com/z-opc-foundation/z-team) | 数字员工协同域——全栈独立仓，自 z-opc 毕业 |
| [z-trade](https://github.com/z-opc-foundation/z-trade) | 交易域独立仓——自 z-opc reactor 迁出 |
| [z-util](https://github.com/z-opc-foundation/z-util) | 底层 Java 工具库——89 个模块 POM，Java 8/17 口径，按需引用按需升级 |
| [z-vector](https://github.com/z-opc-foundation/z-vector) | 自研 Java 向量数据库——嵌入式+可独立部署，ANN+Payload 过滤，兼容 Qdrant REST |
| [z-voice-demo](https://github.com/z-opc-foundation/z-voice-demo) | 语音链路 demo——流式识别→kernel LLM→流式 TTS→可打断 的契约验证实现 |
| [z-watermark](https://github.com/z-opc-foundation/z-watermark) | 水印引擎——显式文字/图片隐写/盲水印，三种技术成对「打+验」 |
| [z-webide](https://github.com/z-opc-foundation/z-webide) | 企业级 WebIDE 后端——Git 仓/共享目录秒变 code-server 实例，全生命周期管理 |
| [z-wf](https://github.com/z-opc-foundation/z-wf) | 自研流程引擎——不依赖 Camunda/Flowable，定义层与运行时全自研 |

**已退役仓**（迁至 [z-trash](https://github.com/z-trash) 组织，archived 只读）：

| 仓 | 说明 |
|---|---|
| [z-agent-proxy](https://github.com/z-trash/z-agent-proxy) | 三方 coding agent 适配代理（Aider/Cursor → kernel Agent SPI），2026-10-26 退役 |
| [z-kb](https://github.com/z-trash/z-kb) | 自研知识库系统（对标 LightRAG/RAGFlow），2026-11 并入 z-cognition 认知舱后退役 |
| [z-ontology](https://github.com/z-trash/z-ontology) | 本体层（TBox/ABox/RDFS 推理），并入 z-cognition 后退役 |

另有 [z-report](https://github.com/z-opc-foundation/z-report)（报表域，2026-10-11 撤销并入 z-lc（远端已删除））。

总清单权威持有处在 [z-opc-foundation-lead](https://github.com/z-opc-foundation/z-opc-foundation-lead) `008_组织规范/005_兄弟仓清单.md`。
