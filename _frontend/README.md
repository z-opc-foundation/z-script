# _frontend/ · 前端两层容器

> z-script 的前端工程目录，按组织规范
> `z-opc-foundation-lead/005_技术架构/005_前端工程与中间件部署架构规范.md` §1.2 落地：
> 一个容器目录 + 两个互不依赖构建系统的 npm 子项目。

## 是什么

- `_frontend/` 本身**不是** npm 项目（没有 `package.json`），只放一份容器级 `.gitignore`
  （统管 `node_modules/`、`dist/`、frontend-maven-plugin 自装的 `node/`），两个子项目继承。
- 两个子项目都是**纯 npm 项目，不进 Maven reactor**；前端与 Java 的接缝只发生在
  `z-script-admin/pom.xml` 里（见下「Maven 侧编排」）。
- 技术栈：React 18 + antd 5 + Vite 5，源码用 `.jsx`（规范 §3.0 允许「可选用 JSX」）。
  仓内不混用 Vue，因此组件层可被上层（z-opc 等）直接 `import`。

## 目录结构

```
_frontend/
├── .gitignore                             # 容器级：node_modules / dist / node
├── z-script-frontend/                     # 应用层 SPA（private，嵌 jar）
│   ├── package.json                       # @yuku123/z-script-frontend
│   ├── vite.config.js                     # base=/script/ + proxy /script → 8086
│   ├── index.html
│   └── src/{main.jsx, App.jsx, index.css}
└── z-script-frontend-component/           # 组件层 library（纯展示）
    ├── package.json                       # @yuku123/z-script-frontend-component
    ├── vite.config.js                     # build.lib, ES only, external react/antd
    └── src/{index.jsx, ScriptListView.jsx, MockEndpointListView.jsx}
```

## 依赖关系

```
z-script-frontend-component  --build-->  dist/index.js (ESM)
        ^
        | "file:../z-script-frontend-component"  +  vite resolve.alias 指向其 dist/index.js
z-script-frontend  --build-->  dist/{index.html, assets/*}
        ^
        | maven-resources-plugin: copy-frontend-dist（process-resources）
z-script-admin  -->  target/classes/static/  -->  z-script-admin-1.0.0-exec.jar
```

应用层 `vite.config.js` 在 `file:` 之外**额外**把
`@yuku123/z-script-frontend-component` alias 到 `../z-script-frontend-component/dist/index.js`：
npm 对 `file:` 依赖可能落成「安装时刻的 copy」而非软链（注释里点名实测 npm 11 +
`--install-links` 仍是 copy），copy 里没有 `dist/` 就会解析失败。alias 是这条时序问题的兜底。

## 本地开发命令

```bash
# 组件层（library watch）
cd _frontend/z-script-frontend-component && npm run dev      # vite build --watch

# 应用层（SPA dev server，5173，host 0.0.0.0）
cd _frontend/z-script-frontend && npm run dev
```

可用脚本只有 package.json 里那几个：应用层 `dev` / `build` / `build:component` / `preview`；
组件层 `dev` / `build` / `preview`。

## 构建链与产物去向

应用层 `npm run build` 已经把时序串好：

```
build            = npm run build:component && vite build
build:component  = cd ../z-script-frontend-component && npm install --silent && npm run build
```

Maven 侧编排（`z-script-admin/pom.xml`，全部路径相对 `${project.basedir}/../_frontend/z-script-frontend`）：

| execution id | 插件 | phase | 作用 |
|---|---|---|---|
| `install-node-and-npm` | frontend-maven-plugin | `generate-resources` | 装 `${node.version}`=v18.17.0 / `${npm.version}`=9.6.7 到 `target/frontend` |
| `npm-install` | frontend-maven-plugin（goal `npm`） | `generate-resources` | `install --no-audit --no-fund` |
| `npm-run-build` | frontend-maven-plugin（goal `npm`） | `generate-resources` | `run build` |
| `copy-frontend-dist` | maven-resources-plugin（goal `copy-resources`） | `process-resources` | `_frontend/z-script-frontend/dist` → `${project.build.outputDirectory}/static` |
| `repackage`（classifier `exec`） | spring-boot-maven-plugin | `package` | `target/z-script-admin-1.0.0-exec.jar` |

```bash
# 一次性出内嵌前端的可执行 jar
mvn -pl z-script-admin -am package -DskipTests
java -jar z-script-admin/target/z-script-admin-1.0.0-exec.jar
# 控制台： http://localhost:8086/script/

# 只装库、本机无 node 时跳过前端（属性定义在仓根 pom.xml）
mvn -pl z-script-admin -am package -DskipTests -Dfrontend.skip=true

# 自检 jar 里确实带上了 static/
unzip -l z-script-admin/target/z-script-admin-1.0.0-exec.jar | grep static
```

`dist/` 与 `target/classes/static/` 都被 `.gitignore` 排除，属于构建产物。

## 与后端 / 部署的三条硬约定

1. **`base` 必须等于 admin 的 `server.servlet.context-path`**。
   `application.yml`：`port: ${SERVER_PORT:8086}` + `context-path: /script`；
   `vite.config.js`：`base: '/script/'`。产物 `dist/index.html` 引用的就是
   `/script/assets/index-*.js`。改任何一边都会 404 —— 这条是 z-schedule 端到端验证踩过的坑
   （规范 §6 端到端记录）。
2. **认证是 z-script 自己的 app + AK**（请求头 `X-Api-Key`），不走 z-ctc 4A / SSO。
   `ZScriptWebMvcConfig` 默认拦 `/api/**`，只放行引导端点 `/api/script/api-key`；
   admin pom 刻意不引 `z-ctc-web`。控制台没有任何登录跳转。
3. **组件层只接 props、不自己 fetch**（规范 §5）。组件层 `src/` 内无 `fetch` / `axios`，
   数据源由应用层决定（真实 admin / 其它实例 / mock）。

## 部署形态

- **Mode 1 合体**（默认）：`dist/` 已在 jar 的 `static/` 里，同源、无 CORS。`make dev`。
- **Mode 2 分体 / Mode 3 集群**：`deploy/Dockerfile.frontend` 把 dist 拷到
  `/usr/share/nginx/html/script`（必须带 `script` 这一层，因为 base 是 `/script/`），
  `deploy/nginx.conf.template` 里 `location /script/api/` 反代到
  `http://${BACKEND_SERVICE}:8086/script/api/`（前后端同前缀，**不做路径重写**），
  `location /script/` 用 `try_files ... /script/index.html` 兜 SPA 路由，
  `location = /` 302 到 `/script/`。`make split` / `make cluster`。

## 常见坑

- 只跑 `vite build` 而组件层没出 `dist/index.js`：alias 指向的是产物文件，必然解析失败；
  走 `npm run build`（它先跑 `build:component`）。
- dev 下代理键是 `/script`（不是 `/script/api`）且无 rewrite，所以后端必须先起在 8086，
  否则接口请求全部失败；后端没起时先确认 8086 端口。
- 首次进控制台是空列表 + `MISSING_API_KEY`，正常：要么点「签发 AK」，要么手工粘 AK。
- 列表全空且无报错：确认 MySQL 已执行 `_doc/002_deploy/init.sql`。
