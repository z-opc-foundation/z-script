# z-script-component · 可复用组件层

> `@yuku123/z-script-component` v0.1.0（`private: true`）。
> Vite **library mode** 产出的纯展示组件包：接 props，不发请求。

## 是什么

z-script 对外复用的视图组件。规范 §5 的约束在这里落地：组件里没有任何
`fetch` / `axios`，数据与回调全由父项目注入，所以同一套组件可以接
z-script admin、别的实例、或者本地 mock 数据。

导出面（`src/index.jsx`，只有两个）：

```jsx
export { default as ScriptListView } from './ScriptListView';
export { default as MockEndpointListView } from './MockEndpointListView';
```

| 组件 | props | 说明 |
|---|---|---|
| `ScriptListView` | `scripts`、`loading`、`onRun(row)`、`onPublish(row)`、`title`（默认「脚本列表」） | antd `Table`，列：`scriptCode` / `scriptName` / `dslType`（Tag 着色）/ `exposeAs`+`httpPath` / `version` / `status`（1=已上线）/ 操作。`rowKey` 取 `scriptCode \|\| id`，分页 `pageSize: 10` |
| `MockEndpointListView` | `endpoints`、`loading`、`onToggle(row, checked)`、`title`（默认「Mock 端点」） | 列：`mockCode` / `mockName` / `method`（Tag 着色）/ `path` / `envCode` / `statusCode` / `delayMs` / 启用 `Switch`（`status ?? enabled ?? 1`）。`rowKey` 取 `mockCode \|\| id` |

两个组件都带 `data-component` 标记（`z-script-script-list`、
`z-script-mock-endpoint-list`），便于跨仓冒烟测试定位 DOM。
回调是可选的：不传 `onRun` / `onPublish` / `onToggle` 时对应按钮/开关不渲染或不动作，
纯只读列表可直接复用。

着色映射（写死在组件里，超出 key 走 `default`）：
`dslType` 支持 `GROOVY` / `JS` / `LUA` / `EL` / `SQL`；`method` 支持
`GET` / `POST` / `PUT` / `DELETE` / `PATCH`。

## 目录结构

```
z-script-component/
├── package.json            # main/module/exports -> ./dist/index.js, files: ["dist"]
├── vite.config.js          # build.lib: src/index.jsx, formats ['es'], rollupOptions.external
└── src/
    ├── index.jsx           # 唯一出口
    ├── ScriptListView.jsx
    └── MockEndpointListView.jsx
```

## 本地开发命令

```bash
cd _frontend/z-script-component
npm run build     # vite build -> dist/index.js
npm run dev       # vite build --watch
```

应用层 `z-script-suit` 的 `npm run build` 会先调 `build:component`，
所以正常全链路构建不需要手工在这里跑命令。

## 依赖与产物约定

- `react` / `react-dom` / `antd` 全在 `peerDependencies`（`antd ^5.0.0`、
  `react ^18.2.0`），devDependencies 里装一份只为本地 build 用。
- external 必须挂在 `rollupOptions.external` 下（`react`、`react-dom`、
  `react/jsx-runtime`、`antd`）。配置注释里记着原因：Vite 没有
  `build.external`，写错位置的后果是 antd/react 被整包打进组件产物（实测 1.75MB）。
- 产物只有 `dist/index.js`（ESM，`formats: ['es']`，`fileName: () => 'index.js'`）。
  `vite.config.js` 注释提到「ES module + d.ts」，但当前配置没有接 `.d.ts` 生成插件，
  `dist/` 里也确实只有 `index.js`；`package.json` 没有 `types` 字段。TS 消费方需要自己声明。

## 怎么被消费

同仓（应用层走 `file:` 协议 + 显式 alias）：

```json
"dependencies": { "@yuku123/z-script-component": "file:../z-script-component" }
```

跨仓（规范 §3.1 默认同样是 `file:`）：

```json
"@yuku123/z-script-component": "file:../../z-script/_frontend/z-script-component"
```

装的时候按规范 §3.4 加 `--install-links`（npm 8+ 不加就是 copy，HMR 失效）。
注意本包 `package.json` 目前是 `private: true`，**不会 `npm publish`**，
跨仓消费只能走 `file:` 或 `npm pack` 出来的 tgz。

用法示例（数据自取，组件不关心来源）：

```jsx
import { ScriptListView } from '@yuku123/z-script-component';

<ScriptListView scripts={rows} loading={loading} onRun={(r) => run(r.scriptCode)} />
```
