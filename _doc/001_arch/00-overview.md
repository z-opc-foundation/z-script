# z-script 架构总览

> L3 中间件仓。本文只写「读代码不易直接得到」的部分：模块边界、鉴权模型、HTTP 面、
> 孵化期间做过的整改及理由。具体实现看各模块源码，部署看 [`deploy/README.md`](../../deploy/README.md)。

## 1. 定位

一件事：**把「脚本」和「Mock」当成平台能力对外提供**。

- 脚本侧：多 DSL 执行（`EL | GROOVY | LUA | SQL | MOCK | API_BRIDGE`）、版本与灰度、标签、执行日志；
- 脚本上线后可被当成 HTTP 端点调用，也可注册成 MCP tool；
- Mock 侧：端点/环境/场景（含状态机）/用例（可批量跑+断言）/录制与回放/curl & OpenAPI 导入、故障注入；
- 对外一律要 `app + AK`，每次调用审计落库；自带一个 Web 控制台。

## 2. 模块与依赖方向

```
z-script (aggregator pom, ${revision} = 1.0.0)
├─ z-script-core        实体 + Mapper + Service（MyBatis-Plus）
├─ z-script-engine      纯引擎：ScriptEngine、sandbox/*、Mock*、Assertion、Recorder、Playback
├─ z-script-web         Controller + 拦截器 + 自动装配（spring.factories）
├─ z-script-scene       聚合 pom
│   └─ z-script-scene-http   HTTP 链路编排（HttpChainExecutor + /api/scene-http）
└─ z-script-admin       可执行应用：内嵌 SPA，exec jar，maven.deploy.skip=true
```

- 依赖只允许向下：`admin → web → engine → core`，`scene-http → engine`。
- 上 Central 的是 `core / engine / web / scene-http` + 聚合 pom；
  **`z-script-admin` 永不上 Central**（架构规范原则 15：可运行应用留在 reactor）。
- 发版走 `-Pcentral`：flatten（`flattenMode=oss`）把 `${revision}` 与 BOM 展开成自包含 pom，
  再 GPG 签名 + `central-publishing-plugin` 发布；父 pom 自带，不依赖任何内部 parent。

## 3. 鉴权模型：只有 app + AK

**这是本仓最强的一条约束**：z-script 不接 z-ctc 4A / SSO。控制台与被调用方走同一套凭证。

```
ApiKeyAuthInterceptor.preHandle  （顺序即语义，任一步失败立即 401/403 并写 errorCode）
 1 提取 X-Api-Key            → 缺失：401 MISSING_API_KEY
 2 按 key 查 z_script_api_key → 不存在：401
 3 status 是否启用            → 禁用：403
 4 过期时间                    → 过期：403
 5 IP 白名单（X-Forwarded-For / X-Real-IP 优先）
 6 scope（能访问哪些脚本）
 7 配额限流
 8 可选 HMAC 签名（X-Timestamp + X-Signature）
 9 配额预警
10 往 request 注入上下文（app/apiKey/quota），异步写 z_script_invoke_log
```

`ZScriptWebMvcConfig` 是**默认拒绝**：`addPathPatterns("/api/**", "/run/**")` + 唯一 exclude
`/api/script/api-key`（POST，创建第一把 Key 的引导端点 —— 此刻调用方还没有 Key，这是逻辑上唯一必须免鉴权的路径）。
`/run/**` 是因为 `ScriptRunController` 这条历史入口不在 `/api` 前缀下，漏点名就等于开了一条免鉴权的脚本执行门。

> 为什么必须是黑名单而不是逐条点名：孵化前这里是 `addPathPatterns("/api/script/**", "/api/mock/**")`
> 一类写法，整套 `/api/mock-platform/**` 管理面**静默免鉴权**。加接口的人不会记得回来补点名。

AK 的 `appName` 绑定在 `z_script_api_key` 上，一个 app 可有多把 Key；`secret` 只在创建/重置时明文返回一次，
库里存的是密文（`ApiKeyServiceImpl`），所以 `init.sql` **刻意不预置任何 Key**（凭证不入仓，见 lead/008）。

## 4. 数据模型（16 表，与 `@TableName` 一一对应）

| 组 | 表 |
|---|---|
| 脚本 | `z_script`、`z_script_version`、`z_script_tag`、`z_script_tag_rel`、`z_script_execution_log` |
| 凭证与治理 | `z_script_api_key`、`z_script_quota`、`z_script_invoke_log` |
| Mock | `z_mock_endpoint`、`z_mock_environment`、`z_mock_scenario`、`z_mock_scenario_state`、`z_mock_test_case`、`z_mock_recording`、`z_mock_recording_request`、`z_mock_request_log` |

建库脚本 [`../002_deploy/init.sql`](../002_deploy/init.sql)：幂等（`CREATE TABLE IF NOT EXISTS` +
`INSERT ... SELECT ... WHERE NOT EXISTS`），末尾带一段 `information_schema` 自检查询，可反复执行。

`Script.status` 与 `Script.exposeAs` 是**两个正交维度**：前者是「有没有上线」（0 草稿 / 1 已上线，
只有 `publish` 会置 1，`unpublish` 会打回 0），后者是「以什么协议对外」（`HTTP / MCP / BOTH / NONE`）。
`ScriptHttpDispatchController` 只按 `exposeAs ∈ {HTTP, BOTH}` 放行，路由键是 `scriptCode` 而不是 `httpPath`
（`httpPath` 只是展示用元数据）。

## 5. 引擎与沙箱

`ScriptEngine.execute(script, params)` 按 `dslType` 分发，**只认 6 种**，其它值直接抛
`Unsupported DSL type`（孵化时 demo 种子脚本写的是 `python`，自检必挂，已改成 `EL`）。

| DSL | 落点 | 限制 |
|---|---|---|
| `EL` | `sandbox/ElSandbox` | SpEL + `SimpleEvaluationContext`（只读数据绑定 + 实例方法）；入参以 `#name` 引用；类型/构造器/bean 引用被拒 → 抛 `ScriptSecurityException` |
| `GROOVY` | `sandbox/GroovySandbox` | AST/策略白名单 `SandboxPolicy` + 超时 `ScriptTimeoutException` |
| `LUA` | `sandbox/` 下 Lua 分支 | 同上策略与超时 |
| `SQL` | 走脚本配置的库 | 只读语义由调用方 scope 约束 |
| `MOCK` | `MockEngine` / `MockTemplateEngine` | 模板渲染 + 场景状态机 |
| `API_BRIDGE` | `DynamicApiExecutor` + `ApiSpecParser` | curl/OpenAPI 转成的外部 HTTP 调用 |

## 6. HTTP 面（全部在 `/api/**` 下，context-path `/script`）

| 前缀 | 内容 |
|---|---|
| `/api/script/**` | 脚本 CRUD/列表/执行/发布/版本与灰度/标签/curl & OpenAPI 导入导出；`api-key`、`quota`、`retry`、`invoke-log` |
| `/api/script-run/{scriptCode}/**` | 已上线脚本的对外调用入口（任意方法，query+header+body 三路合参，body 优先） |
| `/run/{scriptCode}` | 同一能力的历史入口（GET/POST，`Result` 包装），同样受 AK 拦截 |
| `/api/mock/**` | Mock 端点管理与按 path 命中分发 |
| `/api/mock-platform/**` | Mock 平台管理面（端点/环境/场景/用例/curl/openapi/录制/请求日志/统计） |
| `/api/scene-http/chain/{execute,preview}` | HTTP 链路编排 |
| `/actuator/health`、`/actuator/info` | 只暴露这两个，`show-details: never`；`probes.enabled=true` 后 K8s 探针才有 `health/{liveness,readiness}` 分组 |

## 7. 前端两层（`_frontend/`）

- `z-script-frontend-component`：库模式，组件只吃 props、不 fetch（`ScriptListView`、`MockEndpointListView`）；
- `z-script-frontend`：SPA 控制台，负责取数与提交，AK 存 localStorage，请求统一带 `X-Api-Key`；
- **vite `base` 必须等于 admin 的 `server.servlet.context-path`（`/script/`）**，否则内嵌进 jar 后资源 404；
- admin 的 `package` 阶段用 frontend-maven-plugin 装 node/npm、`npm run build`，再由
  maven-resources-plugin 把 dist 拷进 `target/classes/static/` → 一个 jar 同时给 API 和控制台。

细节见 [`../../_frontend/README.md`](../../_frontend/README.md)。

## 8. 孵化期间的整改（为什么和 git blame 长得不一样）

| # | 整改 | 原因 |
|---|---|---|
| 1 | admin 移除 `z-ctc-web` 依赖 | 4A/SSO 会把控制台变成「先跳 /login」，且其 `TenantContextFilter` 强依赖 z-ctc 的装配；中间件必须能独立跑起来。鉴权改由自己的 app + AK 承担 |
| 2 | `ZScriptWebMvcConfig` 改默认拒绝 `/api/**` | 见 §3 的旁路说明 |
| 3 | DB 配置键 `spring.datasource.script.*` → `z.base.db.script.*` | `ModuleDataSourceTemplate` 只读后者；设错不报错，静默回落 `localhost/root/空密码` |
| 4 | 关掉 `MybatisPlusAutoConfiguration` | 多模块各自装配 `SqlSessionFactory`，自动配置会 `expected single matching bean but found 2`；与 z-opc main-starter 同款做法 |
| 5 | 排除 `log4j-slf4j2-impl`（admin 每个依赖） | Boot 2.7 + slf4j-api 1.7 需要 1.x 桥，2.x 桥会导致日志后端抢注 |
| 6 | 显式对齐 JUnit 5.11.4 / platform 1.11.4（放在 BOM import 之前）+ `junit-platform-launcher` | BOM 里的版本与 surefire 3.5.4 不匹配，测试直接不跑 |
| 7 | demo 种子脚本 `dslType: python → EL` | `ScriptEngine` 不认 python，自检/控制台执行必挂 |
| 8 | `unpublish` 补 `status=0` | 只有 `publish` 会置 1，此前取消发布后脚本永远显示「已上线」，控制台也再没有上线入口 |
| 9 | 控制台请求参数改走 query（`scriptCode`） | 后端是 `@RequestParam`，此前 执行/上下线 三个按钮全部 400 |
| 10 | 部署模板：`$ VAR` → `${VAR}`、JRE 17 → 8、`SPRING_DATASOURCE_*` → `Z_BASE_DB_SCRIPT_*`、前端产物落 `html/script`、nginx 反代 `/script/api/` | 复制自 z-schedule 的模板带着它的假设；本仓是 Java 8 编译 + `/script` context-path |
| 11 | K8s：新增 `06-configmap.yaml`，Secret 改为 `k8s-apply.sh` 现场创建 | 占位符 Secret 会覆盖真凭证；凭证不落 Git |
| 12 | `z-script-scene-http` 的 `spring.factories` 补 `EnableAutoConfiguration=` 键 | 文件里只有一行裸类名，自动装配从不生效；admin 靠 `scanBasePackages` 侥幸能用，外部调用方引入依赖后什么都不发生 |
| 13 | 拦截器补点名 `/run/**` | 只拦 `/api/**` 时，`ScriptRunController` 的 `/run/{scriptCode}`（publish 后写进 `http_path` 的那个路径）是**免鉴权就能执行任意已上线脚本**的门。实测：整改前无 Key 200，整改后 401 `MISSING_API_KEY` |
| 14 | `extractScriptCode` 认 `/script-run/` 段 | 它只解析 `/run/`、`/mock/`，于是在推荐入口 `/api/script-run/{code}` 上 scriptCode 恒为 null → `verifyScope` 的 SPECIFIC 分支 `contains(null)` 为 false，**scope=SPECIFIC 的 Key 永远 403**；调用日志的 `script_code` 也一直是空 |
| 15 | 删除 `ScriptRunController./mock/**` | 它用 `getRequestURI().substring(5)` 剥前缀，在 context-path=`/script` 下永远算错 mockPath（→ 恒 404），且功能与 `MockDispatchController` 的 `/api/mock/**` 重复。从未生效，无调用方 |
| 16 | `management.endpoint.health.probes.enabled=true` | K8s 探针打的是 `/script/actuator/health/{liveness,readiness}`，而这两个分组只在识别到 Kubernetes 平台时才自动开启 → 本机/compose 下 404，配置文件与探针各说各话 |
| 17 | `ScriptMyBatisConfig` 去掉 `mapperLocations` | 仓内没有任何 `mapper/*.xml`（mappers 全是 MP `BaseMapper` + 注解），留着只让每次启动多一条 WARN |

## 9. 已知不一致 / 后续

- `publish`/`create` 生成的 `httpPath` 是 `/run/{scriptCode}`（`init.sql` 的 demo 已对齐成同一个值），
  但推荐入口是 `/api/script-run/{scriptCode}`。分发不读 `http_path`，所以它只是展示；要收敛得先定「path 由谁说了算」，
  顺带决定要不要把这两扇门合成一扇。
- `unpublish` 不清 `httpPath`（MyBatis-Plus `updateById` 忽略 null 字段），前端已按 `exposeAs` 规避显示。
- HMAC 签名校验的 `readBody()` 是空实现（恒 `""`），因此带请求体的 POST 只要发 `X-Signature` 就必 401；
  现阶段签名只对 GET / 无体请求可用。要真用签名需先套 `ContentCachingRequestWrapper`（注意 body 只能读一次）。
- `scope` 的 scriptCode 是从 URI 里解析的，管理面端点解析不出 → `SPECIFIC` 的 Key 调不到 `list/publish`；
  给控制台用的 Key 应保持 `ALL`。`READ_ONLY` 目前是空实现（与 ALL 等价），写操作并没有被收窄。
- 控制台目前只覆盖「脚本列表 + Mock 端点列表 + 执行/上下线 + 签发 AK」，
  版本灰度、场景状态机、录制回放还没有 UI；组件库按 props-only 约定继续加即可。
