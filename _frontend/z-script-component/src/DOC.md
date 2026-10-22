# z-script-component

z-script（脚本平台 + Mock 平台）跨仓唯一前端面：把「脚本列表 / 版本灰度 / 场景状态机 / 录制回放 /
应用 / Mock 端点」这一整套脚本与 Mock 平台的视图收敛成一个可复用的 React 组件包；
`z-script-suit` 是它的独立运行壳（真正发请求的应用层）。

- **npm 包名**：`@yuku123/z-script-component`
- **宿主接入**：`@yuku123/z-script-component`（主入口）/ `@yuku123/z-script-component/pages`（manifest 桶）
- **suit**：`@yuku123/z-script-suit`（private，不发布 npm）
- **后端**：z-script-admin 的 `/api/script/**`（脚本平台）与 `/api/mock-platform/**`（Mock 平台）
- **技术栈**：React 19 + antd 6 + react-router-dom 7 + vite（library mode，双入口 `.` / `./pages`）

> **两套包的分工**：`z-script-component` 放**视图层**；`z-script-suit` 放**应用层**
> （登录、首页、控制台页、菜单、路由表）。菜单数组在 suit 侧，不在 component 侧。

---

## 一、能力概览

### 1.1 component 包视图（`z-script-component/src/`）

全部是**接 props 的纯展示组件**（规范 §5：组件层不自己 fetch，数据源由父项目决定）。

| 视图 | 源文件 | props | 能力 |
|------|--------|-------|------|
| 脚本列表 | `ScriptListView.jsx` | `scripts` / `loading` / `onRun` / `onPublish` / `title` | 脚本编码、名称、DSL（GROOVY/JS/LUA/EL/SQL）、暴露方式、版本 |
| 版本 / 灰度 | `ScriptVersionView.jsx` | `selectedScriptId` / `versions` / `loading` / 回调 | 版本号、状态（生产/灰度/草稿/下线）、灰度比例、是否当前版本、changelog |
| 场景 / 状态机 | `ScenarioView.jsx` | `request` | 场景列表、实例状态、端点、用例、删除 / 重置实例 |
| 录制 / 回放 | `RecordingView.jsx` | `request` | 录制列表、请求明细、开始 / 停止录制、回放、比对、导入 |
| 应用 | `AppListView.jsx` | `apps` / `loading` / `onEditScripts` / `onToggle` | 应用编码、名称、权限范围（scope）、允许的脚本列表、启停 |
| Mock 端点 | `MockEndpointListView.jsx` | `endpoints` / `loading` / `onToggle` | 编码、名称、Method、Path、环境、状态码、延迟、启停 |

> ⚠ `ScenarioView` 与 `RecordingView` **不是纯 props**：它们接一个 `request(path, init)` 函数
> prop，内部自己调 `mock-platform/...`（路径相对 `/api/`）。其余四个视图才是纯展示。

### 1.2 suit 包页面（`z-script-suit/src/`）

| 页面 | 路由（相对 `/z-script/*`） | 源文件 | 能力 |
|------|--------------------------|--------|------|
| 登录 | `login` | `LoginPage.jsx` | 登录页（suit 侧单独挂这条路由） |
| 首页 | `home` | `HomePage.jsx` | 入口卡片页 |
| 脚本控制台 | `console` | `ConsolePage.jsx` | 真正的应用页：AK 鉴权、脚本列表 / 运行 / 上下线 / 应用配置 / 版本发布与灰度 |
| 组件文档 | `docs` | 来自 component 包的 `ComponentDocPage` | 本文件渲染页（左侧 240px TOC + 右侧正文） |

整个 AK 控制台是**有状态单页**（共享 apiKey 与脚本选中态），所以 suit 侧只占一个菜单项。

### 1.3 component 的骨架路由（`src/pages-manifest.jsx`）

| 路由 | 视图 |
|------|------|
| `/z-script/script` | `ScriptListView` |
| `/z-script/script/version` | `ScriptVersionView` |
| `/z-script/script/scenario` | `ScenarioView` |
| `/z-script/script/recording` | `RecordingView` |
| `/z-script/script/app` | `AppListView` |
| `/z-script/script/mock` | `MockEndpointListView` |
| `/z-script/docs` | `ComponentDocPage`（本次新增） |

这些是**供主壳探测 domainRoutes 的骨架清单**：页面文件当时还住在 suit 里，正式消费请走 suit。

## 二、导出面

```js
// 主入口（. 桶）：六个视图
import {ScriptListView, ScriptVersionView, ScenarioView, RecordingView, AppListView, MockEndpointListView}
    from '@yuku123/z-script-component'

// manifest 桶（./pages）
import {routes, appMeta, ComponentDocPage} from '@yuku123/z-script-component/pages'
```

## 三、API 端点

**组件层不发请求**（`ScenarioView` / `RecordingView` 通过 `request` prop 由父层注入）。
下面是两个真实消费方的路径。

### 3.1 suit 控制台（`z-script-suit/src/ConsolePage.jsx`）

请求前缀 = `import.meta.env.BASE_URL` + `api/`（suit 的 vite base 是 `/script/`），
每次请求带 `X-Api-Key` 头。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `api/script/api-key` | 用当前 app + 凭据换一个 AK |
| POST | `api/script/run?scriptCode=` | 运行脚本 |
| POST | `api/script/{next}?scriptCode=` | 脚本上下线（`next` 是状态动作） |
| GET | `api/script/app?appCode=` | 应用详情 |
| GET | `api/script/app/scripts?appCode=` | 应用可用的脚本列表 |
| GET | `api/script/version/list/{scriptId}` | 脚本的版本列表 |
| POST | `api/script/version/publish?...` | 发布版本 |
| POST | `api/script/version/promote/{versionId}` | 灰度提升 |
| POST | `api/script/version/offline/{versionId}` | 版本下线 |

### 3.2 组件层（`ScenarioView` / `RecordingView` 内部，路径相对 `/api/`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `mock-platform/scenarios/list` | 场景列表（裸数组） |
| GET | `mock-platform/scenarios?scenarioCode=` | 场景详情 |
| GET | `mock-platform/scenarios/instances?scenarioCode=` | 场景实例状态（裸 map：`instanceKey → currentState`） |
| GET | `mock-platform/endpoints/list?scenarioCode=` | 场景下的端点（裸数组） |
| GET | `mock-platform/cases/list?caseGroup=` | 场景下的用例（裸数组） |
| GET | `mock-platform/recordings/list` | 录制列表（裸数组） |
| GET | `mock-platform/recordings?recordingCode=` | 录制详情 |
| GET | `mock-platform/recordings/requests?recordingCode=` | 录制的请求明细（裸数组） |
| POST | `mock-platform/recordings/start` | 开始录制 |
| POST | `mock-platform/recordings/stop?recordingCode=` | 停止录制 |
| GET | `mock-platform/recordings/playback?recordingCode=` | 回放结果 `[{recordingCode, target, total, pass, fail, results}]` |
| GET | `mock-platform/recordings/compare?recordingCode=` | 比对结果 `{success, total, match, diff, diffs}` |
| GET | `mock-platform/recordings/import?recordingCode=` | 导入录制 |

## 四、页面说明

### 4.1 脚本列表

展示脚本编码、名称、DSL、暴露方式、版本。DSL 配色：GROOVY 紫 / JS 蓝 / LUA 红 / EL 绿 / SQL 橙。
「暴露方式」列只在真按 HTTP / BOTH 暴露时才带上 `httpPath` —— 取消发布后 `httpPath` 仍留在库里
（MyBatis-Plus `updateById` 不写 null），不判一下会出现「NONE /demo/hello」这种脏显示。

### 4.2 版本 / 灰度

版本号、状态（`PUBLISHED` 生产 / `GRAY` 灰度 / `DRAFT` 草稿 / `DEPRECATED` 下线）、灰度比例、
是否当前版本、changelog。数据形状来自 `ScriptVersionController`：
`id / scriptId / versionNo / status / grayPercentage / isCurrent / changelog / createTime`。

### 4.3 场景 / 状态机

场景列表（点行进详情）、实例状态、关联端点与用例；支持删除场景（不会删除关联端点）与
重置实例到初始状态。状态色按状态名里的关键字判定（finish/done/complete/success ⇒ 绿，
fail/error/cancel/reject ⇒ 红，start/running/pending/processing ⇒ 蓝，其余紫）。

### 4.4 录制 / 回放

录制列表（点行进详情）、请求明细、开始 / 停止录制（停止后不能再采集）、回放、比对、导入。

### 4.5 应用

应用是权限模型的中心（AK 挂应用、脚本列表挂应用）。列：应用编码、名称、权限范围（scope）、
允许的脚本列表、启停。编辑动作全部由父层通过 `onEditScripts` / `onToggle` 回调决定。

### 4.6 Mock 端点

列：编码、名称、Method（GET 蓝 / POST 绿 / PUT 橙 / DELETE 红 / PATCH 紫）、Path、环境、
状态码、延迟（ms）、启停开关。

### 4.7 组件文档

本文件。内容以 `?raw` 在编译期内联，零额外请求；左侧 240px TOC 随滚动联动。

## 五、配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| vite `base` | `'/script/'` | **必须**与 admin 的 `server.servlet.context-path` 一致，否则 `index.html` 里的 `/assets/xxx.js` 404 |
| router basename | `BASE \|\| undefined` | suit 侧，来自 `src/routes-base.js` |
| AK 存储键 | `z-script-console-api-key` | localStorage；每次请求带 `X-Api-Key` |
| 登录态判据 | localStorage `token` | suit 的 `ProtectedShell` 用 |
| 组件层请求前缀 | 相对 `/api/` | `ScenarioView` / `RecordingView` 的 `request` prop 约定 |
| 响应形状兜底 | 裸数组 / `{data}` / `{rows}` 三种都兜 | suit 的 `rowsOf()` 一次处理，避免前端猜结构 |

## 六、已知限制

- **z-script 只有 app + AK 一种认证方式**：`ApiKeyAuthInterceptor` 默认拒绝 `/api/**`，
  控制台自己不例外 —— AK 存 localStorage，每次请求带 `X-Api-Key`。
  返回 401 时页面抛的是 `body.errorCode`（如 `MISSING_API_KEY`），不是通用文案。
- **`ScenarioView` / `RecordingView` 不是纯 props 组件**（它们接 `request` 函数 prop 并自己发请求），
  与规范 §5「组件层只接 props」有出入；`ScriptListView` / `ScriptVersionView` / `AppListView` /
  `MockEndpointListView` 才是纯展示。
- **组件的 `pages-manifest.jsx` 里没有 `menuItems`** —— 菜单数组在 suit 侧
  （`z-script-suit/src/pages-manifest.jsx`）。加菜单项要改 suit。
- component 的 manifest 里那六条路由是**骨架**（页面文件当时还住在 suit 里），
  与 suit 的 `/z-script/console` 不是同一套东西。
- `z-script-frontend/` 与 `z-script-frontend-component/` 两个目录**只剩 `dist/` 与 `node_modules/`**，
  没有源码 —— 是历史残留，不要往里加文件。
- 后端统一响应可能是裸数组 / `{data}` / `{rows}`，suit 侧用 `rowsOf()` 兜一层；
  组件层（`ScenarioView` / `RecordingView`）直接按裸数组 / 裸 map 处理，两边口径不同。
- 组件包必须 external 基座；`vite.config.js` 的 `external` 是精确字符串匹配，
  `@yuku123/z-frontend-common/ui` 与 `/ui/echarts` 两个子路径必须逐个列出，漏了基座会被复制进 dist。

## 七、变更记录

| 版本 | 变更 |
|---|---|
| 0.1.1 | 新增组件文档页 `ComponentDocPage`（lead 008 §7）：`src/DOC.md` + 基座 `ComponentDoc` 薄包装；suit 菜单与路由接上 |
| 0.1.0 | 首版：脚本 / 版本 / 场景 / 录制 / 应用 / Mock 端点六个纯展示视图 + 骨架 manifest |
