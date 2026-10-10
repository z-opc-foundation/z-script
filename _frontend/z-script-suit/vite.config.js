import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

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
    plugins: [react()],
    resolve: {
    dedupe: ['react', 'react-dom', 'react-router-dom', 'antd', '@ant-design/icons', 'axios'],
        alias: {
            '@': path.resolve(__dirname, 'src'),
            // 组件层走 file: 协议（规范 §3.1 默认）。npm 对 file: 依赖可能落成"安装时刻的副本"
            // 而非软链（实测 npm 11 + --install-links 仍是 copy），副本里没有 dist 就会解析失败。
            // 因此显式 alias 到组件层产物，绕开 copy/symlink 时序问题，同时保留 package.json 的 file: 声明。
            '@yuku123/z-script-component': path.resolve(
                __dirname,
                '../z-script-component/dist/index.js'
            ),
        },
    },
    server: {
        port: 5173,
        host: '0.0.0.0',
        proxy: {
            // dev: /script/api/** → 本机 admin（同 context-path，无需 rewrite）
            '/script': {
                target: 'http://localhost:8086',
                changeOrigin: true,
            },
        },
    },
    build: {
        outDir: 'dist',
        assetsDir: 'assets',
        emptyOutDir: true,
        sourcemap: false,
    },
});
