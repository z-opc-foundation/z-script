# z-script 1.0.0 孵化入库审计

> 审计日期：2026-09-20
> 审计人：qoder（孵化执行方）
> 关联规范：`z-opc-foundation-lead/005_技术架构/005_前端工程与中间件部署架构规范.md`、
> `006_中间件部署形态规范`、`008_组织规范/001_凭证管理规范.md`
> 审计范围：z-script 全仓（core / engine / web / scene-http / admin + `_frontend/` 两层 + `deploy/`）

---

## 1. 扫描摘要

| # | 扫描项 | 结果 | 说明 |
|---|--------|------|------|
| 1 | LICENSE 合规 | ✅ PASS | 仓根 `LICENSE` 与 `pom.xml` 的 `<license>` 一致（MIT），子模块继承 |
| 2 | 版本与自包含 | ✅ PASS | `${revision}=1.0.0`，全仓 pom 无 SNAPSHOT 依赖；flatten-maven-plugin `oss` 模式 + `-Pcentral` |
| 3 | 可运行模块不入 Central | ✅ PASS | `z-script-admin/pom.xml:34` `maven.deploy.skip=true`（架构原则 15） |
| 4 | 敏感信息扫描 | ✅ PASS | yml/sh/sql/k8s 无硬编码口令；只有 `Z_BASE_DB_SCRIPT_PASSWORD`、`YOUR_DB_PASSWORD` 这类占位；K8s Secret 由 `k8s-apply.sh` 现场 `kubectl create secret`，仓库只留 ConfigMap |
| 5 | Git 历史合规 | ✅ PASS | 历史只有 `first commit`（仅跟踪 README.md），本次为孵化后的首次完整入库，入库前 `git status` 逐项核对 |
| 6 | 忽略规则 | ✅ PASS | 补 `.gitignore`：`target/`、`node_modules/`、`dist/`、frontend-maven-plugin 自装的 `node/`、`.idea/`、`*.iml`、日志与本地 env 文件 |
| 7 | 鉴权面扫描 | ⚠️ 已整改 | 发现并关闭 1 条免鉴权的脚本执行旁路 + 1 条 scope 永远 403 的解析缺陷，见 §2 WARN-001/002 |
| 8 | 构建与测试 | ✅ PASS | `mvn -pl z-script-admin -am package`（JDK 17 编译 / Java 8 目标）→ BUILD SUCCESS；surefire 36 用例 0 失败（engine 15 + scene-http 21） |
| 9 | 端到端运行 | ✅ PASS | JDK 8 起 exec jar：4.4s、31 行日志、0 WARN；`/script/`、`doc.html`、`swagger-ui`、`v3/api-docs`、`actuator/health{,/liveness,/readiness}` 全 200 |
| 10 | 部署模板自洽 | ✅ PASS | 3 份 compose `docker compose config` 通过；K8s 清单 `envsubst` 渲染后无残留 `${}`、YAML 可解析；`bin/*.sh` 全部 `bash -n` 通过 |
| 11 | 静态分析 / 覆盖率 | ⚠️ NOT_RUN | 未跑 SpotBugs/SonarQube 与 JaCoCo（本机环境限制，非结论） |
| 12 | 第三方依赖 CVE | ⚠️ WARN | `spring-boot 2.7.12` 已过 OSS 维护期末段；`knife4j 4.1.0 / springdoc 1.6.15` 由 z-boot-web-starter 传递引入，本仓不掌握升级节奏 |

**总结：10 PASS / 2 WARN / 0 ERROR，另有 2 项产品代码缺陷在审计中修复（WARN-001/002）**

## 2. 问题列表

### WARN-001：`/run/{scriptCode}` 是免鉴权的脚本执行入口（已修）
- **级别**: ERROR 级安全问题，已关闭
- **模块**: z-script-web
- **文件**: `config/ZScriptWebMvcConfig.java`、`controller/ScriptRunController.java`
- **描述**: 拦截器只点名 `/api/**`，而 `ScriptRunController` 的 `/run/{scriptCode}` 不在该前缀下 ——
  它恰恰是 `publish` 写进 `http_path` 的那个路径。整改前实测：不带 `X-Api-Key` 直接 200 拿到脚本输出。
  同一文件里另有 `/mock/**`，同样免鉴权。
- **修复**: `addPathPatterns("/api/**", "/run/**")`；`/mock/**` 因与 `MockDispatchController` 功能重复
  且剥前缀写法在 `/script` context-path 下恒 404（从未生效），直接删除。
- **验证**: 无 Key `GET /script/run/hello_world` → 401 `MISSING_API_KEY`；带 Key → 200 `"hello, zifang! @ z-script"`；
  `GET /script/mock/anything` → 404。

### WARN-002：`scope=SPECIFIC` 的 Key 在推荐入口上恒 403（已修）
- **级别**: 功能缺陷
- **模块**: z-script-web
- **文件**: `interceptor/ApiKeyAuthInterceptor.java#extractScriptCode`
- **描述**: scriptCode 只从 `/run/`、`/mock/` 两种 URI 形态里解析，推荐入口 `/api/script-run/{code}` 解析结果为 null，
  `ApiKeyServiceImpl.verifyScope` 的 SPECIFIC 分支于是拿 `contains(null)` 判定 → 恒 false。
  副作用：`z_script_invoke_log.script_code` 在该入口下一直是空。
- **修复**: 前缀表加 `/script-run/`，并统一「取到下一段为止」的截断（此前带子路径的 `/{code}/**` 会把子路径一起带进 code）。
- **验证**: `allowed_scripts=["hello_world"]` 的 Key 调 `/api/script-run/hello_world` → 200，调 `/api/script-run/not_allowed` → 403 `SCOPE_NOT_ALLOWED`；日志表 `script_code` 已正确落值。

### WARN-003：K8s 探针路径与配置不一致（已修）
- **文件**: `z-script-admin/src/main/resources/application.yml`、`deploy/k8s/01-deployment-backend.yaml`
- **描述**: 清单里 liveness/readiness 打的是 `/script/actuator/health/{liveness,readiness}`，
  而这两个健康分组只在识别到 Kubernetes 平台时才自动开启 → 本机/compose 下 404，配置与探针各说各话。
- **修复**: 显式 `management.endpoint.health.probes.enabled=true`；三个 health 路径实测均 200。

### WARN-004：HMAC 签名对带 body 的请求不可用（未修，已记录）
- **文件**: `interceptor/ApiKeyAuthInterceptor.java#readBody`（恒返回 `""`）
- **描述**: 签名串含 body，服务端却拿不到 body（流已被读且未缓存），因此 POST 带体 + `X-Signature` 必 401。
  当前签名是可选步骤（不带 `X-Timestamp`/`X-Signature` 就跳过），不影响主链路。
- **建议**: 需要签名时用 `ContentCachingRequestWrapper`（Filter 侧包装，注意 body 单次可读）；
  同时把 HMAC key 从 `api_secret_hash` 换成明文 secret（现方案在注释里已自陈是折衷）。

### WARN-005：文档型 WARN
- 接口文档（Knife4j 4.1.0 / springdoc 1.6.15）是 z-boot-web-starter **传递**带来的，本仓 pom 未声明；
  上游 starter 升级会连带影响 `/script/doc.html` 与 `/script/v3/api-docs`。已在 `_doc/002_deploy/z-script-admin.md` 写明。
- 控制台 UI 目前只覆盖脚本列表 / Mock 端点列表 / 执行 / 上下线 / 签发 AK；
  版本灰度、场景状态机、录制回放无 UI。属已知范围边界，非缺陷。

## 3. 复核命令

```bash
# 构建 + 测试（编译用 17，运行用 8）
JAVA_HOME=$JAVA_HOME_17 mvn -B -pl z-script-admin -am package
grep -h "Tests run" */target/surefire-reports/*.txt

# 起服务并验证鉴权面
$JAVA_HOME_8/bin/java -jar z-script-admin/target/z-script-admin-1.0.0-exec.jar
curl -s -o /dev/null -w '%{http_code}\n' 'http://localhost:8086/script/run/hello_world?name=x'   # 期望 401
curl -s -X POST http://localhost:8086/script/api/script/api-key \
     -H 'Content-Type: application/json' -d '{"appName":"audit","scope":"ALL"}'                  # 期望 200

# 部署模板
cd deploy && docker compose --profile with-db config -q && bash -n bin/*.sh
```

## 4. 结论

放行入库。核心风险集中在「鉴权覆盖面」而非功能正确性：z-script 的 AK 链路本身实现完整
（IP 白名单 / scope / 配额 / 审计日志），但默认拒绝的边界一度漏掉了自己的运行时路由。
后续任何新增 Controller 请保持落在 `/api/**` 下，否则必须回到 `ZScriptWebMvcConfig` 点名。
