import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import {safeEmptyOutDir} from '../../build-tools/safe-vite-plugin.mjs'

const localSiblings = process.env.LOCAL_SIBLINGS === '1';
const workspaceRoot = path.resolve(__dirname, '../../..');

// Z-Script 控制台应用层。
//
// ⚠️ base 必须与 admin 的 server.servlet.context-path 一致（/script/），
// 否则 index.html 引用的 /assets/xxx.js 会 404（应为 /script/assets/xxx.js）。
// 这是 z-schedule 端到端验证时踩过的坑，见 lead/005 §6 端到端记录。
//
// 构建产物 dist/ 由 z-script-admin pom 的 maven-resources-plugin 在 process-resources
// 阶段复制到 target/classes/static/，随 exec.jar 一起发布（Mode 1 合体部署）。
export default defineConfig({
    base: '/script/',
    plugins: [safeEmptyOutDir(), react()],
    resolve: {
        dedupe: ['react', 'react-dom', 'react-router-dom', 'antd', '@ant-design/icons', 'axios'],
        alias: [
            { find: '@', replacement: path.resolve(__dirname, 'src') },
            ...(localSiblings ? [
                { find: /^@yuku123\/z-script-component\/pages$/, replacement: path.resolve(__dirname, '../z-script-component/src/pages.js') },
                { find: /^@yuku123\/z-script-component$/, replacement: path.resolve(__dirname, '../z-script-component/src/index.jsx') },
                { find: /^@yuku123\/z-frontend-common\/ui$/, replacement: path.resolve(workspaceRoot, 'z-frontend-common/src/components/ui/index.js') },
                { find: /^@yuku123\/z-frontend-common$/, replacement: path.resolve(workspaceRoot, 'z-frontend-common/src/index.js') },
            ] : []),
        ],
    },
    server: {
        port: 5173,
        host: '0.0.0.0',
        fs: { allow: [localSiblings ? workspaceRoot : path.resolve(__dirname, '..')] },
        proxy: {
            // dev: /script/api/** → 本机 admin（同 context-path，无需 rewrite）
            '^/script/api(?:/|$)': {
                target: 'http://localhost:8086',
                changeOrigin: true,
            },
        },
    },
    build: {
        outDir: 'dist',
        assetsDir: 'assets',
        emptyOutDir: false,
        sourcemap: false,
    },
});
