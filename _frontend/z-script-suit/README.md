# z-script-suit · 控制台应用层（SPA）

> `@yuku123/z-script-suit`，`private: true`，不发布。
> 唯一职责：装配页面 + 管认证 + 决定数据从哪来，展示交给组件层。

## 是什么

一个不带路由库的单页控制台：`Layout` + 顶部 AK 工具栏 + 两个 `Tabs`
（脚本 / Mock 端点）。入口链路：

```
index.html  →  src/main.jsx（ReactDOM.createRoot + ConfigProvider locale=zhCN）
            →  src/App.jsx（全部业务装配）
```

`App.jsx` 从组件层拿两个视图：

```jsx
import { ScriptListView, MockEndpointListView } from '@yuku123/z-script-component';
```

## 本地开发命令

```bash
cd _frontend/z-script-suit
npm run build:component   # 先出组件层 dist（alias 依赖它）
npm run dev               # vite dev server： port 5173, host 0.0.0.0
npm run build             # = npm run build:component && vite build
npm run preview           # vite preview
```

dev 代理（`vite.config.js`）：

```js
proxy: { '/script': { target: 'http://localhost:8086', changeOrigin: true } }
```

即 `/script/**`（含 `/script/api/**`）**原样**转发到本机 admin，无需 rewrite
—— 两边 context-path 相同。因此 dev 前要先起后端：

```bash
mvn -pl z-script-admin -am package -DskipTests
java -jar z-script-admin/target/z-script-admin-1.0.0-exec.jar
```

## 认证：app + AK，不是 SSO

- 所有请求经 `api(p) = ${import.meta.env.BASE_URL}api/${p}`，也就是
  `/script/api/**`；base 变了接口前缀跟着变，没有硬编码域名。
- 唯一认证方式是请求头 `X-Api-Key`。AK 存 localStorage，键名 `z-script-console-api-key`；
  启动时自动读回，`Input.Password` 可手工粘贴覆盖。
- 首次使用点右上角「签发 AK」：`POST /script/api/script/api-key`，
  body `{ appName, scope: 'ALL', description }`。这是后端唯一免鉴权的引导端点
  （`ZScriptWebMvcConfig` 拦 `/api/**`，exclude 只写了这一个精确路径）。
- 响应 `data.apiKey` 写入 localStorage 并用于后续请求；`data.plainSecret` 只在 message 里
  显示一次，不落存储。
- 后端 401 时 `request()` 抛 `body.errorCode`（如 `MISSING_API_KEY`），页面顶部 Alert
  按错误码给不同提示。没有登录页、没有重定向。

实际用到的后端端点：

| 调用点 | 方法与路径 |
|---|---|
| `load()` | `GET /script/api/script/list`、`GET /script/api/mock-platform/endpoints/list` |
| 签发 AK | `POST /script/api/script/api-key` |
| 执行 | `POST /script/api/script/run` |
| 上线/下线 | `POST /script/api/script/publish`、`/script/api/script/unpublish` |

`rowsOf()` 负责兜后端响应结构（裸数组 / `data` / `rows` / `data.list`），组件层只拿到数组。

## 构建产物去哪

`build.outDir = dist`、`assetsDir = assets`、`emptyOutDir: true`、`sourcemap: false`。
产物不入库（`_frontend/.gitignore`），由 `z-script-admin/pom.xml` 的
`maven-resources-plugin:copy-frontend-dist`（`process-resources`）复制到
`z-script-admin/target/classes/static/`，随 `spring-boot-maven-plugin:repackage` 进入
`z-script-admin-1.0.0-exec.jar`（Mode 1 合体部署）。
Mode 2/3 走 `deploy/Dockerfile.frontend`，dist 被拷成 nginx 的 `html/script/`。

## 常见坑

- `base: '/script/'` 与 `server.servlet.context-path: /script` 必须同步改，
  否则 `index.html` 引用的 `/assets/*.js` 会 404（应为 `/script/assets/*.js`）。
  访问地址始终是 `http://localhost:8086/script/`，不是根路径。
- 组件层没 build 就 `npm run dev`：vite alias 指向
  `../z-script-component/dist/index.js`，文件不存在直接解析失败。
- 执行/发布按钮：`runScript` / `publishScript` 把 `scriptCode` 放在 JSON body 里，
  而 `ScriptController` 的 `/run`、`/publish`、`/unpublish` 用 `@RequestParam` 取参。
  若后端回 400，就是这个参数位置不一致导致的，需要改成 query 传参。
- 「签发 AK」后不要手动再 `load()`：`apiKey` 变化会让 `useEffect` 用新 AK 重拉，
  手动调用会拿到旧闭包里的空 AK，产生一个后到的 401 假错误（代码注释已标注）。
